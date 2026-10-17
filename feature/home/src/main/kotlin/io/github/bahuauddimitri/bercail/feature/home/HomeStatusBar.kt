package io.github.bahuauddimitri.bercail.feature.home

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.LifecycleStartEffect

/**
 * Hides the phone's own status bar (time, battery, network) while [hidden]: on Écoute only. Sliding a finger down
 * from the top edge shows it for a moment. It comes back as soon as [hidden] is false, and whenever the home screen
 * is no longer on screen; it hides again when the home comes back while still [hidden].
 */
@Composable
internal fun StatusBarHiddenWhile(hidden: Boolean) {
    val window = LocalActivity.current?.window
    if (hidden && window != null) {
        LifecycleStartEffect(window) {
            val bars = WindowCompat.getInsetsController(window, window.decorView)
            bars.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            bars.hide(WindowInsetsCompat.Type.statusBars())
            onStopOrDispose { bars.show(WindowInsetsCompat.Type.statusBars()) }
        }
    }
}
