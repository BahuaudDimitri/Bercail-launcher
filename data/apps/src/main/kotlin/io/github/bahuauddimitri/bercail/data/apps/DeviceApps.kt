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
import io.github.bahuauddimitri.bercail.core.domain.Picture
import io.github.bahuauddimitri.bercail.core.domain.apps.App
import io.github.bahuauddimitri.bercail.core.domain.apps.AppShortcut
import io.github.bahuauddimitri.bercail.core.domain.apps.AppsSource
import io.github.bahuauddimitri.bercail.core.domain.apps.IconPack
import io.github.bahuauddimitri.bercail.core.domain.apps.PackCatalog
import io.github.bahuauddimitri.bercail.core.domain.apps.PackIcon
import io.github.bahuauddimitri.bercail.core.domain.apps.iconGlyph
import io.github.bahuauddimitri.bercail.core.domain.apps.monochromeGlyph
import io.github.bahuauddimitri.bercail.core.domain.apps.packGlyph
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
class DeviceApps internal constructor(
    context: Context,
    private val background: CoroutineDispatcher,
    packFiles: IconPackFiles?
) : AppsSource {
    constructor(context: Context, background: CoroutineDispatcher = Dispatchers.IO) : this(context, background, null)

    private val context = context.applicationContext
    private val packages = this.context.packageManager
    private val packs = IconPacks(packages, packFiles ?: InstalledIconPackFiles(packages), this.context.packageName)

    /** A signal each time a package is installed, removed or changed, and once at first; only while listened to. */
    private val changes: Flow<Unit> = callbackFlow {
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
    }.conflate()

    override val apps: Flow<List<App>> = changes.map { withContext(background) { installed() } }.distinctUntilChanged()

    /** The icon packs, looked for only while someone listens: the settings. */
    override val iconPacks: Flow<List<IconPack>> =
        changes.map { withContext(background) { packs.installed() } }.distinctUntilChanged()

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

    private val glyphs = Glyphs(this.context, packs)

    /**
     * The drawing chosen for the app, or the one the chosen icon pack gives it; without one, its own glyph
     * ([Glyphs.of]). Drawn once per version of the app and of the pack: an update may bring a new icon.
     */
    override suspend fun glyph(appId: String, iconPack: String?, drawing: PackIcon?): Picture? =
        withContext(background) {
            ComponentName.unflattenFromString(appId)?.let { glyphs.of(appId, it, iconPack, drawing) }
        }

    override suspend fun iconCatalog(pack: IconPack): PackCatalog = withContext(background) { packs.catalog(pack) }

    override suspend fun packIconGlyph(icon: PackIcon): Picture? = withContext(background) { glyphs.ofPackIcon(icon) }

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
 * A drawing of an icon pack as a white glyph, [frameDp] square, framed as the dock's own glyphs: drawn over the
 * middle three quarters of the frame, a pack's drawing, which leaves its own margin, comes out at about the size of
 * a monochrome layer's (about 28 dp in the dock's 56 dp frame).
 */
internal fun Drawable.packGlyph(context: Context, frameDp: Int): Picture? {
    val size = (frameDp * context.resources.displayMetrics.density).roundToInt()
    return packGlyph(drawn(size, asLayer = false, margin = size / PACK_MARGIN_PARTS))
}

/** A pack's drawing leaves an eighth of the frame empty on each side. */
private const val PACK_MARGIN_PARTS = 8

/** The glyph of a shortcut stands in a 32 dp ring of the menu: drawn a little larger, it is only made smaller. */
private const val SHORTCUT_GLYPH_DP = 44

/**
 * The drawable as plain pixels, [size] square. A layer of an adaptive icon is half as large again as the icon,
 * whose edges only show its middle: [asLayer] frames it the same way.
 */
private fun Drawable.drawn(size: Int, asLayer: Boolean, margin: Int = 0): Picture {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val bleed = if (asLayer) size / LAYER_BLEED else 0
    val edge = margin - bleed
    setBounds(edge, edge, size - edge, size - edge)
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
