package io.github.bahuauddimitri.bercail.data.apps

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.util.LruCache
import io.github.bahuauddimitri.bercail.core.domain.Picture
import io.github.bahuauddimitri.bercail.core.domain.apps.App
import io.github.bahuauddimitri.bercail.core.domain.apps.AppShortcut
import io.github.bahuauddimitri.bercail.core.domain.apps.AppsSource
import io.github.bahuauddimitri.bercail.core.domain.apps.iconGlyph
import io.github.bahuauddimitri.bercail.core.domain.apps.monochromeGlyph
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * The apps installed on the phone. Android is asked for the list when someone starts listening, then tells when
 * an app is installed, removed or changed; nothing stays registered once nobody listens. Reading names and
 * drawing icons happens away from the main thread.
 */
class DeviceApps(context: Context, private val background: CoroutineDispatcher = Dispatchers.IO) : AppsSource {
    private val context = context.applicationContext
    private val packages = this.context.packageManager

    override val apps: Flow<List<App>> = callbackFlow {
        val launcher = checkNotNull(this@DeviceApps.context.getSystemService(LauncherApps::class.java))
        val changes = object : LauncherApps.Callback() {
            override fun onPackageAdded(packageName: String, user: UserHandle) = changed()
            override fun onPackageRemoved(packageName: String, user: UserHandle) = changed()
            override fun onPackageChanged(packageName: String, user: UserHandle) = changed()
            override fun onPackagesAvailable(names: Array<String>, user: UserHandle, replacing: Boolean) = changed()
            override fun onPackagesUnavailable(names: Array<String>, user: UserHandle, replacing: Boolean) = changed()

            private fun changed() {
                trySend(Unit)
            }
        }
        launcher.registerCallback(changes, Handler(Looper.getMainLooper()))
        trySend(Unit)
        awaitClose { launcher.unregisterCallback(changes) }
    }.conflate().map { withContext(background) { installed() } }.distinctUntilChanged()

    override suspend fun icon(appId: String): Picture? = withContext(background) {
        val component = ComponentName.unflattenFromString(appId) ?: return@withContext null
        val drawable = try {
            packages.getActivityIcon(component)
        } catch (_: PackageManager.NameNotFoundException) {
            return@withContext null
        }
        val size = (ICON_DP * this@DeviceApps.context.resources.displayMetrics.density).roundToInt()
        drawable.drawn(size, asLayer = false)
    }

    /**
     * The monochrome layer Android 13 asks apps for, framed as the icon is; without it, the real icon in one
     * colour. Drawn once per version of the app: an update may bring a new icon.
     */
    override suspend fun glyph(appId: String): Picture? = withContext(background) {
        val component = ComponentName.unflattenFromString(appId) ?: return@withContext null
        val updated = try {
            packages.getPackageInfo(component.packageName, PackageManager.PackageInfoFlags.of(0)).lastUpdateTime
        } catch (_: PackageManager.NameNotFoundException) {
            NEVER_UPDATED
        }
        glyphs.get(appId)?.takeIf { it.updated == updated }?.let { return@withContext it.glyph }
        val drawable = try {
            packages.getActivityIcon(component)
        } catch (_: PackageManager.NameNotFoundException) {
            return@withContext null
        }
        val glyph = drawable.glyph(context, DOCK_GLYPH_DP)
        glyphs.put(appId, Drawn(updated, glyph))
        glyph
    }

    /** A glyph drawn for one version of an app; nothing when it has none that reads. */
    private class Drawn(val updated: Long, val glyph: Picture?)

    /**
     * The glyphs drawn lately: only the [GLYPHS_KEPT] apps seen last, about 5.5 MB, so that a long list of apps does
     * not fill the memory. An app let go is drawn again when it shows.
     */
    private val glyphs = LruCache<String, Drawn>(GLYPHS_KEPT)

    override fun open(appId: String) {
        val component = ComponentName.unflattenFromString(appId) ?: return
        val launch = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        context.startSafely(launch)
    }

    private val shortcuts = DeviceShortcuts(this.context)

    override suspend fun shortcuts(appId: String): List<AppShortcut> = withContext(background) { shortcuts.of(appId) }

    override suspend fun shortcutGlyph(shortcut: AppShortcut): Picture? =
        withContext(background) { shortcuts.iconOf(shortcut)?.glyph(context, SHORTCUT_GLYPH_DP) }

    override fun openShortcut(shortcut: AppShortcut) = shortcuts.open(shortcut)

    /** Every activity Android offers to open from a launcher, except Bercail's own. */
    private fun installed(): List<App> = packages
        .queryIntentActivities(LAUNCHABLE, PackageManager.ResolveInfoFlags.of(0))
        .filter { it.activityInfo.packageName != context.packageName }
        .map { entry ->
            val component = ComponentName(entry.activityInfo.packageName, entry.activityInfo.name)
            App(id = component.flattenToShortString(), name = entry.loadLabel(packages).toString())
        }

    private companion object {
        val LAUNCHABLE: Intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        /** Icons are drawn once, a little larger than the 34 dp they are shown at. */
        const val ICON_DP = 40

        /** A package Android tells nothing about: its glyph is drawn once and kept. */
        const val NEVER_UPDATED = -1L

        /** How many glyphs are kept: enough for the dock, the media sheet and a screen of the search. */
        const val GLYPHS_KEPT = 64
    }
}

/**
 * The drawable in one colour, [frameDp] square: the monochrome layer of an adaptive icon, framed as the icon is;
 * without it, the whole icon in one colour.
 */
internal fun Drawable.glyph(context: Context, frameDp: Int): Picture? {
    val size = (frameDp * context.resources.displayMetrics.density).roundToInt()
    val monochrome = (this as? AdaptiveIconDrawable)?.monochrome
    return if (monochrome != null) {
        monochromeGlyph(monochrome.drawn(size, asLayer = true))
    } else {
        iconGlyph(drawn(size, asLayer = false))
    }
}

/**
 * The glyph of an app is drawn at the size the dock shows it: a 56 dp frame (`BcDock`), its drawing, the middle
 * half, at about 28 dp. Drawn smaller, it would be enlarged on screen, and blur.
 */
private const val DOCK_GLYPH_DP = 56

/** The glyph of a shortcut stands in a 32 dp ring of the menu: drawn a little larger, it is only made smaller. */
private const val SHORTCUT_GLYPH_DP = 44

/**
 * The drawable as plain pixels, [size] square. A layer of an adaptive icon is half as large again as the icon,
 * whose edges only show its middle: [asLayer] frames it the same way.
 */
private fun Drawable.drawn(size: Int, asLayer: Boolean): Picture {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val bleed = if (asLayer) size / LAYER_BLEED else 0
    setBounds(-bleed, -bleed, size + bleed, size + bleed)
    draw(Canvas(bitmap))
    val pixels = IntArray(size * size)
    bitmap.getPixels(pixels, 0, size, 0, 0, size, size)
    bitmap.recycle()
    return Picture(width = size, height = size, pixels = pixels)
}

/** A layer spills over each edge of the icon by a quarter of its width. */
private const val LAYER_BLEED = 4

/** Starts an activity of another app; an app that is gone or refuses is not an error for the launcher. */
internal fun Context.startSafely(intent: Intent) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Nothing to open: the screen simply stays where it is.
    } catch (_: SecurityException) {
        // The other app does not let itself be opened this way.
    }
}
