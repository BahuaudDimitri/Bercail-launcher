package io.github.bahuauddimitri.bercail.data.apps

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.ShortcutQuery
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.os.Process
import io.github.bahuauddimitri.bercail.core.domain.apps.AppShortcut

/**
 * The shortcuts the apps offer into themselves, as Android gives them to the default home app only. They are read
 * when asked, on a long press in the dock: nothing is listened to, nothing is kept.
 */
internal class DeviceShortcuts(private val context: Context) {
    private val launcher: LauncherApps? get() = context.getSystemService(LauncherApps::class.java)

    /** The enabled shortcuts of an app; none while Bercail is not the default home app. */
    fun of(appId: String): List<AppShortcut> {
        val component = ComponentName.unflattenFromString(appId) ?: return emptyList()
        val query = ShortcutQuery().setPackage(component.packageName).setActivity(component).setQueryFlags(ALL)
        return read(query).filter { it.isEnabled }.map { it.asShortcut(appId) }
    }

    /** The icon the app gives its shortcut, if it is still there. */
    fun iconOf(shortcut: AppShortcut): Drawable? {
        val component = ComponentName.unflattenFromString(shortcut.appId) ?: return null
        val query = ShortcutQuery()
            .setPackage(component.packageName)
            .setShortcutIds(listOf(shortcut.id))
            .setQueryFlags(ALL)
        return read(query).firstOrNull()?.let { launcher?.getShortcutIconDrawable(it, densityDpi) }
    }

    private val densityDpi: Int get() = context.resources.displayMetrics.densityDpi

    /** Opens the app on its shortcut, as it would open from its own icon. */
    fun open(shortcut: AppShortcut) {
        val component = ComponentName.unflattenFromString(shortcut.appId) ?: return
        try {
            launcher?.startShortcut(component.packageName, shortcut.id, null, null, Process.myUserHandle())
        } catch (_: ActivityNotFoundException) {
            // The shortcut leads nowhere any more: the screen simply stays where it is.
        } catch (_: IllegalStateException) {
            // The shortcut was turned off, or the phone is still locked after a restart.
        } catch (_: SecurityException) {
            // Bercail is no longer the default home app.
        }
    }

    /** What Android answers; nothing while Bercail is not the default home app, or the phone is still locked. */
    private fun read(query: ShortcutQuery): List<ShortcutInfo> = try {
        val launcher = launcher
        if (launcher != null && launcher.hasShortcutHostPermission()) {
            launcher.getShortcuts(query, Process.myUserHandle()).orEmpty()
        } else {
            emptyList()
        }
    } catch (_: SecurityException) {
        emptyList()
    } catch (_: IllegalStateException) {
        emptyList()
    }

    private fun ShortcutInfo.asShortcut(appId: String) = AppShortcut(
        appId = appId,
        id = id,
        label = (shortLabel ?: longLabel)?.toString().orEmpty(),
        kind = when {
            isDeclaredInManifest -> AppShortcut.Kind.Manifest
            isDynamic -> AppShortcut.Kind.Dynamic
            else -> AppShortcut.Kind.Pinned
        },
        rank = rank
    )

    private companion object {
        /** The shortcuts the app declares, those it offers as it is used, and those pinned on a home screen. */
        const val ALL = ShortcutQuery.FLAG_MATCH_DYNAMIC or ShortcutQuery.FLAG_MATCH_MANIFEST or
            ShortcutQuery.FLAG_MATCH_PINNED
    }
}
