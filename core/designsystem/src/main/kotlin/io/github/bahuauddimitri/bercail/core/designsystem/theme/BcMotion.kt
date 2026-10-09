package io.github.bahuauddimitri.bercail.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable
import kotlin.math.roundToInt

/**
 * Animation durations and curve, scaled by Android's animation speed.
 * At scale 0 ("remove animations"), everything is instant and continuous animations (Brume) stand still.
 */
@Immutable
data class BcMotion(
    val drawerMillis: Int,
    val sheetMillis: Int,
    val fadeMillis: Int,
    val timelineMillis: Int,
    val easing: Easing,
    val reduced: Boolean
) {
    companion object {
        private const val DRAWER = 400
        private const val SHEET = 350
        private const val FADE = 250
        private const val TIMELINE = 500

        /** The prototype's curve: fast start, long soft landing. */
        val Easing: Easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

        fun forAnimatorScale(scale: Float): BcMotion {
            fun scaled(millis: Int) = (millis * scale).roundToInt()
            return BcMotion(
                drawerMillis = scaled(DRAWER),
                sheetMillis = scaled(SHEET),
                fadeMillis = scaled(FADE),
                timelineMillis = scaled(TIMELINE),
                easing = Easing,
                reduced = scale == 0f
            )
        }
    }
}
