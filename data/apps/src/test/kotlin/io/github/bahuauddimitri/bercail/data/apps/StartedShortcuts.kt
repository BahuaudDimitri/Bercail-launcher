package io.github.bahuauddimitri.bercail.data.apps

import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.Bundle
import android.os.UserHandle
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowLauncherApps

/** Android's launcher service, which here only notes the shortcuts it is asked to start. */
@Implements(LauncherApps::class)
class StartedShortcuts : ShadowLauncherApps() {
    @Implementation
    override fun startShortcut(
        packageName: String,
        shortcutId: String,
        sourceBounds: Rect?,
        startActivityOptions: Bundle?,
        user: UserHandle
    ) {
        started += "$packageName/$shortcutId"
    }

    companion object {
        val started = mutableListOf<String>()
    }
}
