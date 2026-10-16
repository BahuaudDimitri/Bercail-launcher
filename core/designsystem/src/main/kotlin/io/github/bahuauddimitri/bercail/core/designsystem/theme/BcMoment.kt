package io.github.bahuauddimitri.bercail.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/**
 * The moments of the day, each with its accent: the colors of Brume, peach in the morning, blue-green by day,
 * lavender in the evening. One accent per screen: the past of the line of the day, the ring of now, and later the
 * unread counts, the equalizer, the main button.
 */
enum class BcMoment(internal val accent: Color) {
    Morning(BcColors.pastelApricot),
    Day(BcColors.pastelMint),
    Evening(BcColors.pastelLavender);

    /** The soft glow behind what is drawn in the accent. */
    internal val halo: Color get() = accent.copy(alpha = HALO_ALPHA)
}

/** Gives [content] the accent of [moment]. Without it, a screen takes the accent of the day. */
@Composable
fun BcMomentScope(moment: BcMoment, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalBcMoment provides moment, content = content)
}

private const val HALO_ALPHA = 0.55f
