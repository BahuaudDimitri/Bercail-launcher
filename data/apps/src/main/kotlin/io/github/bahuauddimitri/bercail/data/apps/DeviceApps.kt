package io.github.bahuauddimitri.bercail.data.apps

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import io.github.bahuauddimitri.bercail.core.domain.apps.App
import io.github.bahuauddimitri.bercail.core.domain.apps.AppIcon
import io.github.bahuauddimitri.bercail.core.domain.apps.AppsSource
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

    override suspend fun icon(appId: String): AppIcon? = withContext(background) {
        val component = ComponentName.unflattenFromString(appId) ?: return@withContext null
        val drawable = try {
            packages.getActivityIcon(component)
        } catch (_: PackageManager.NameNotFoundException) {
            return@withContext null
        }
        val size = (ICON_DP * this@DeviceApps.context.resources.displayMetrics.density).roundToInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(Canvas(bitmap))
        val pixels = IntArray(size * size)
        bitmap.getPixels(pixels, 0, size, 0, 0, size, size)
        bitmap.recycle()
        AppIcon(width = size, height = size, pixels = pixels)
    }

    override fun open(appId: String) {
        val component = ComponentName.unflattenFromString(appId) ?: return
        val launch = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        context.startSafely(launch)
    }

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
