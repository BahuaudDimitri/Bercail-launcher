package io.github.bahuauddimitri.bercail.core.designsystem.brume

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import io.github.bahuauddimitri.bercail.core.designsystem.motion.MAX_ANIMATION_FPS
import kotlinx.coroutines.delay

/**
 * Slows the whole screen down to 20 Hz while Brume is the only thing moving. Measured on the Pixel 9: it takes
 * both the window's preferred refresh rate and the view's vote; either alone, or the Compose layer vote, leaves
 * 60 Hz. During a touch and for one second after it, the screen keeps its normal rate so the drawer and other
 * animations stay smooth.
 */
@Composable
internal fun SlowScreenWhileAlone(state: BcBrumeState) {
    val view = LocalView.current
    val window = view.context.findActivity()?.window
    var calm by remember { mutableStateOf(true) }
    LaunchedEffect(state.wakes) {
        if (state.wakes == 0) return@LaunchedEffect
        calm = false
        delay(TOUCH_SETTLE_MILLIS)
        calm = true
    }
    val slow = state.isAnimating && calm
    DisposableEffect(view, window, slow) {
        if (slow) {
            window?.setPreferredRefreshRate(MAX_ANIMATION_FPS.toFloat())
            view.requestedFrameRate = MAX_ANIMATION_FPS.toFloat()
        }
        onDispose {
            window?.setPreferredRefreshRate(NO_PREFERENCE)
            view.requestedFrameRate = View.REQUESTED_FRAME_RATE_CATEGORY_DEFAULT
        }
    }
}

private fun Window.setPreferredRefreshRate(rate: Float) {
    attributes = attributes.apply { preferredRefreshRate = rate }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private const val TOUCH_SETTLE_MILLIS = 1_000L
private const val NO_PREFERENCE = 0f
