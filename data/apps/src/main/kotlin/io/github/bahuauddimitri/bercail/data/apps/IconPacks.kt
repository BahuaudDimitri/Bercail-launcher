package io.github.bahuauddimitri.bercail.data.apps

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.Xml
import io.github.bahuauddimitri.bercail.core.domain.apps.IconPack
import io.github.bahuauddimitri.bercail.core.domain.apps.PackCatalog
import io.github.bahuauddimitri.bercail.core.domain.apps.PackIcon
import java.io.IOException
import java.io.InputStream
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

/** What an icon pack answers, the ADW/Nova standard every launcher reads packs by. */
internal const val ICON_PACK_ACTION = "org.adw.launcher.THEMES"

/**
 * Which drawing of a pack each app gets, from its `appfilter`: the launch component of the app, then the name of the
 * drawable. A pack keeps it [compiled] among its resources (`res/xml/appfilter`) or as a plain [file] in its assets;
 * the compiled one is read first. The first entry of a component wins; entries that name no component are left.
 */
internal fun readAppFilter(compiled: XmlPullParser?, file: () -> InputStream?): Map<ComponentName, String> = try {
    compiled?.items() ?: file()?.use { Xml.newPullParser().apply { setInput(it, null) }.items() }.orEmpty()
} catch (_: XmlPullParserException) {
    emptyMap()
} catch (_: IOException) {
    emptyMap()
}

private fun XmlPullParser.items(): Map<ComponentName, String> {
    val found = HashMap<ComponentName, String>()
    while (next() != XmlPullParser.END_DOCUMENT) {
        if (eventType == XmlPullParser.START_TAG && name == "item") {
            val component = getAttributeValue(null, "component")?.let(::componentOf)
            val drawable = getAttributeValue(null, "drawable")
            if (component != null && !drawable.isNullOrBlank()) found.putIfAbsent(component, drawable)
        }
    }
    return found
}

/** `ComponentInfo{package/package.Activity}` as Android names it; `:BROWSER` and the like name no app. */
private fun componentOf(entry: String): ComponentName? =
    ComponentName.unflattenFromString(entry.trim().removePrefix("ComponentInfo{").removeSuffix("}"))

/**
 * Every drawing a pack offers, from its `drawable.xml`: [compiled] among its resources (`res/xml/drawable`) or as a
 * plain [file] in its assets, the compiled one first. Each `<item drawable="…"/>` once, in the order of the file;
 * the categories that group them are left.
 */
internal fun readDrawableList(compiled: XmlPullParser?, file: () -> InputStream?): List<String> = try {
    compiled?.drawables() ?: file()?.use { Xml.newPullParser().apply { setInput(it, null) }.drawables() }.orEmpty()
} catch (_: XmlPullParserException) {
    emptyList()
} catch (_: IOException) {
    emptyList()
}

private fun XmlPullParser.drawables(): List<String> {
    val found = LinkedHashSet<String>()
    while (next() != XmlPullParser.END_DOCUMENT) {
        if (eventType == XmlPullParser.START_TAG && name == "item") {
            getAttributeValue(null, "drawable")?.takeIf { it.isNotBlank() }?.let(found::add)
        }
    }
    return found.toList()
}

/** What an installed icon pack holds: which drawing each app gets, and the drawings. */
internal interface IconPackFiles {
    /** Which drawing each app gets in the [pack] ([readAppFilter]); nothing when the pack is gone. */
    fun appFilter(pack: String): Map<ComponentName, String>?

    /** Every drawing the [pack] lists in its `drawable.xml` ([readDrawableList]); nothing when the pack is gone. */
    fun drawables(pack: String): List<String>?

    /** The drawable of the [pack] by its [name]; nothing when it has none by that name. */
    fun drawing(pack: String, name: String): Drawable?
}

/**
 * The files of the packs installed on the phone, read from their resources. Those of another app can only be found
 * by their name, as the `appfilter` gives them: hence the lookups by name lint discourages.
 */
@SuppressLint("DiscouragedApi")
internal class InstalledIconPackFiles(private val packages: PackageManager) : IconPackFiles {
    override fun appFilter(pack: String): Map<ComponentName, String>? = resourcesOf(pack)?.let { resources ->
        val compiled = resources.getIdentifier(APP_FILTER, "xml", pack).takeIf { it != 0 }?.let(resources::getXml)
        readAppFilter(compiled) { resources.assets.openOrNull("$APP_FILTER.xml") }
    }

    override fun drawables(pack: String): List<String>? = resourcesOf(pack)?.let { resources ->
        val compiled = resources.getIdentifier(DRAWABLE, "xml", pack).takeIf { it != 0 }?.let(resources::getXml)
        readDrawableList(compiled) { resources.assets.openOrNull("$DRAWABLE.xml") }
    }

    override fun drawing(pack: String, name: String): Drawable? = resourcesOf(pack)?.let { resources ->
        val id = resources.getIdentifier(name, "drawable", pack)
        try {
            if (id == 0) null else resources.getDrawable(id, null)
        } catch (_: Resources.NotFoundException) {
            null
        }
    }

    private fun resourcesOf(pack: String): Resources? = try {
        packages.getResourcesForApplication(pack)
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    private fun AssetManager.openOrNull(path: String): InputStream? = try {
        open(path)
    } catch (_: IOException) {
        null
    }

    private companion object {
        const val APP_FILTER = "appfilter"
        const val DRAWABLE = "drawable"
    }
}

/**
 * The icon packs of the phone. The `appfilter` of a pack is read once, when the dock first asks for it, and again
 * only when the pack is updated or another one is chosen: only the last one read is kept.
 */
internal class IconPacks(
    private val packages: PackageManager,
    private val files: IconPackFiles,
    private val own: String
) {
    private var read: ReadFilter? = null

    /** The packs installed, by the name of their app, from A to Z; never Bercail. */
    fun installed(): List<IconPack> = packages
        .queryIntentActivities(Intent(ICON_PACK_ACTION), PackageManager.ResolveInfoFlags.of(0))
        .map { it.activityInfo }
        .filter { it.packageName != own }
        .distinctBy { it.packageName }
        .map { IconPack(id = it.packageName, name = it.applicationInfo.loadLabel(packages).toString()) }
        .sortedBy { it.name.lowercase() }

    /** When the [pack] was installed or last updated; nothing when it is gone. */
    fun updated(pack: String): Long? = try {
        packages.getPackageInfo(pack, PackageManager.PackageInfoFlags.of(0)).lastUpdateTime
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    /** The drawing the [pack], at the version [updated], gives the app opened by [component]; nothing if none. */
    fun drawingOf(pack: String, updated: Long, component: ComponentName): Drawable? =
        filterOf(pack, updated)[component]?.let { files.drawing(pack, it) }

    /** One drawing of a pack, by its name; nothing when the pack is gone or has no drawing by that name. */
    fun drawing(icon: PackIcon): Drawable? = files.drawing(icon.pack, icon.name)

    /**
     * Every drawing the [pack] offers, from its `drawable.xml`, or else those its `appfilter` names, and the one it
     * gives each app. Read each time it is asked: only the icon picker asks, when it opens.
     */
    fun catalog(pack: IconPack): PackCatalog {
        val filter = files.appFilter(pack.id).orEmpty()
        val listed = files.drawables(pack.id).orEmpty().ifEmpty { filter.values.toList() }
        return PackCatalog(pack, listed.distinct(), filter.mapKeys { it.key.flattenToShortString() })
    }

    @Synchronized
    private fun filterOf(pack: String, updated: Long): Map<ComponentName, String> {
        val kept = read?.takeIf { it.pack == pack && it.updated == updated }
        val filter = kept?.filter ?: files.appFilter(pack).orEmpty()
        if (kept == null) read = ReadFilter(pack, updated, filter)
        return filter
    }

    /** The `appfilter` of one version of a pack. */
    private class ReadFilter(val pack: String, val updated: Long, val filter: Map<ComponentName, String>)
}
