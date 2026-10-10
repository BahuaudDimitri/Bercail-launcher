package io.github.bahuauddimitri.bercail.feature.home

import android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.testTag
import io.github.bahuauddimitri.bercail.core.designsystem.brume.BcBrume
import io.github.bahuauddimitri.bercail.core.designsystem.brume.BcBrumeColors
import io.github.bahuauddimitri.bercail.core.designsystem.brume.BcBrumeState
import io.github.bahuauddimitri.bercail.core.designsystem.brume.BcWallpaperVeil
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.domain.media.MediaState
import io.github.bahuauddimitri.bercail.core.domain.screen.HomeBackground

/**
 * What is behind the home screen: Brume in the colors of the cover while a content is loaded, fading in and out
 * over the user's wallpaper. While Brume covers the screen, the system no longer draws the wallpaper under it.
 */
@Composable
internal fun HomeBackdrop(state: HomeUiState, brume: BcBrumeState) {
    val fade = tween<Float>(BcTheme.motion.fadeMillis)
    val wanted = remember(state.background, state.dayPart) {
        (state.background as? HomeBackground.Brume)?.let { it.cover?.brume ?: state.dayPart.brume }
    }
    // While Brume fades out, it keeps the colors it had.
    val last = remember { Ref<BcBrumeColors>() }
    if (wanted != null) last.value = wanted
    val shown = last.value
    val opacity by animateFloatAsState(if (wanted != null) 1f else 0f, fade, label = "brume")

    SystemWallpaper(visible = opacity < 1f)
    // Under Brume, over the wallpaper: the agenda stays readable on any picture.
    if (opacity < 1f) BcWallpaperVeil(Modifier.fillMaxSize().testTag(VEIL_TAG))
    if (shown != null && opacity > 0f) {
        val first by animateColorAsState(shown.first, tween(BcTheme.motion.fadeMillis), label = "brume first")
        val second by animateColorAsState(shown.second, tween(BcTheme.motion.fadeMillis), label = "brume second")
        BcBrume(
            colors = remember(first, second) { BcBrumeColors.of(first, second) },
            playing = state.media.isPlaying,
            modifier = Modifier.fillMaxSize().testTag(BRUME_TAG).graphicsLayer { alpha = opacity },
            state = brume,
            weather = state.weather.look,
            night = state.dayPart.isDark,
            // A new track wakes Brume up, like a touch.
            wakeKey = (state.media as? MediaState.Loaded)?.content
        )
    }
}

/** Asks the system to stop drawing the wallpaper under the home screen while it is not [visible] anyway. */
@Composable
private fun SystemWallpaper(visible: Boolean) {
    val window = LocalActivity.current?.window ?: return
    DisposableEffect(window, visible) {
        val hides = !visible && window.attributes.flags and FLAG_SHOW_WALLPAPER != 0
        if (hides) window.clearFlags(FLAG_SHOW_WALLPAPER)
        onDispose { if (hides) window.addFlags(FLAG_SHOW_WALLPAPER) }
    }
}
