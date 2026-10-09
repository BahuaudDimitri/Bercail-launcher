package io.github.bahuauddimitri.bercail.core.designsystem.brume

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.motion.rememberAnimationSeconds
import io.github.bahuauddimitri.bercail.core.designsystem.motion.rememberPowerSaveMode
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

/** Weather drawn over Brume. The system wallpaper keeps its own native weather effects. */
enum class BcWeather { Clear, Rain, Wind, Snow }

/**
 * The two colors of Brume (from the album cover, or of the moment), always kept dark:
 * bright blobs cost battery on an OLED screen.
 */
@Immutable
class BcBrumeColors private constructor(val first: Color, val second: Color) {
    companion object {
        fun of(first: Color, second: Color) = BcBrumeColors(first.keptDark(), second.keptDark())

        /** Colors of the moment, when nothing plays: peach in the morning, blue-green by day, lavender at night. */
        val Morning = of(Color(0xFFFFBF8F), Color(0xFFFF9A8A))
        val Day = of(Color(0xFF8FC4FF), Color(0xFF86E0C4))
        val Evening = of(Color(0xFFB9A6FF), Color(0xFF6A6FF0))
    }
}

/** Brightest a Brume color may be (relative luminance). */
internal const val MAX_LUMINANCE = 0.55f

/** Whether Brume moves, and what wakes it up again. */
@Stable
class BcBrumeState {
    internal var wakes by mutableIntStateOf(0)

    /** True while Brume draws new frames; false when still (paused, idle, battery saver, animations off). */
    var isAnimating by mutableStateOf(false)
        internal set

    /** A touch on the screen: Brume moves again for 10 seconds. */
    fun wake() {
        wakes++
    }
}

@Composable
fun rememberBcBrumeState(): BcBrumeState = remember { BcBrumeState() }

/** Wakes Brume on every touch of this area, without taking the touch away from what is underneath. */
fun Modifier.wakesBrume(state: BcBrumeState): Modifier = pointerInput(state) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.type == PointerEventType.Press) state.wake()
        }
    }
}

/**
 * The Brume background: four soft blobs in two colors, drifting slowly, with the weather on top and a dark veil
 * that keeps the agenda readable. It moves at 20 frames per second only while music plays, and stands still after
 * 10 seconds without a touch, in battery saver, when Android removes animations, or off screen.
 */
@Composable
fun BcBrume(
    colors: BcBrumeColors,
    playing: Boolean,
    modifier: Modifier = Modifier,
    state: BcBrumeState = rememberBcBrumeState(),
    weather: BcWeather = BcWeather.Clear,
    night: Boolean = false,
    wakeKey: Any? = null
) {
    val reduced = BcTheme.motion.reduced
    val powerSave by rememberPowerSaveMode()
    val allowed = playing && !reduced && !powerSave
    LaunchedEffect(allowed, state.wakes, wakeKey) {
        state.isAnimating = allowed
        if (allowed) {
            delay(IDLE_FREEZE_MILLIS)
            state.isAnimating = false
        }
    }
    DisposableEffect(state) { onDispose { state.isAnimating = false } }
    val seconds by rememberAnimationSeconds(running = state.isAnimating, start = START_SECONDS)

    Canvas(modifier.clipToBounds().clearAndSetSemantics {}) {
        drawSky(night)
        if (night && (weather == BcWeather.Clear || weather == BcWeather.Wind)) drawStars(seconds)
        drawBlobs(seconds, colors, wind = weather == BcWeather.Wind, night = night)
        when (weather) {
            BcWeather.Rain -> drawRain(seconds)
            BcWeather.Wind -> drawGusts(seconds)
            BcWeather.Snow -> drawSnow(seconds)
            BcWeather.Clear -> Unit
        }
        drawTopVeil()
    }
}

private fun DrawScope.drawSky(night: Boolean) {
    val (top, bottom) = if (night) BcColors.nightTop to BcColors.nightBottom else DAY_TOP to DAY_BOTTOM
    drawRect(Brush.verticalGradient(listOf(top, bottom)))
}

private fun DrawScope.drawBlobs(t: Float, colors: BcBrumeColors, wind: Boolean, night: Boolean) {
    val speed = if (wind) WIND_SPEED else CALM_SPEED
    val reach = if (wind) WIND_REACH else CALM_REACH
    val alpha = if (night) NIGHT_ALPHA else DAY_ALPHA
    val radius = size.width * BLOB_RADIUS
    BLOBS.forEachIndexed { index, blob ->
        val color = if (blob.firstColor) colors.first else colors.second
        val center = Offset(
            x = size.width * (blob.x + reach * sin(t * speed * (1 + index * DRIFT_STEP) + blob.phase)),
            y = size.height * (blob.y + BLOB_SWAY * cos(t * speed * SWAY_SPEED + blob.phase * PHASE_SKEW))
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)), center, radius),
            radius = radius,
            center = center,
            blendMode = BlendMode.Plus
        )
    }
}

private fun DrawScope.drawTopVeil() {
    val height = TOP_VEIL.toPx()
    drawRect(
        brush = Brush.verticalGradient(
            0f to BcColors.nightTop.copy(alpha = VEIL_TOP),
            VEIL_MIDDLE_STOP to BcColors.nightTop.copy(alpha = VEIL_MIDDLE),
            1f to BcColors.nightTop.copy(alpha = 0f),
            endY = height
        ),
        size = size.copy(height = height)
    )
}

private fun Color.keptDark(): Color {
    if (luminance() <= MAX_LUMINANCE) return this
    var low = 0f
    var high = 1f
    repeat(DARKEN_STEPS) {
        val middle = (low + high) / 2
        if (scaled(middle).luminance() > MAX_LUMINANCE) high = middle else low = middle
    }
    return scaled(low)
}

private fun Color.scaled(factor: Float) = Color(red * factor, green * factor, blue * factor, alpha)

private data class Blob(val x: Float, val y: Float, val phase: Float, val firstColor: Boolean)

/** The prototype's four blobs: start position, phase, color. */
private val BLOBS = listOf(
    Blob(x = 0.25f, y = 0.28f, phase = 0f, firstColor = true),
    Blob(x = 0.75f, y = 0.42f, phase = 2f, firstColor = false),
    Blob(x = 0.55f, y = 0.72f, phase = 4f, firstColor = true),
    Blob(x = 0.2f, y = 0.62f, phase = 1.3f, firstColor = false)
)

private const val IDLE_FREEZE_MILLIS = 10_000L
private const val START_SECONDS = 12f
private const val CALM_SPEED = 0.12f
private const val WIND_SPEED = 0.55f
private const val CALM_REACH = 0.16f
private const val WIND_REACH = 0.32f
private const val DAY_ALPHA = 0.28f
private const val NIGHT_ALPHA = 0.22f
private const val BLOB_RADIUS = 0.62f
private const val BLOB_SWAY = 0.1f
private const val DRIFT_STEP = 0.25f
private const val SWAY_SPEED = 0.8f
private const val PHASE_SKEW = 1.3f
private const val VEIL_TOP = 0.85f
private const val VEIL_MIDDLE = 0.45f
private const val VEIL_MIDDLE_STOP = 0.75f
private const val DARKEN_STEPS = 12
private val TOP_VEIL = 230.dp
private val DAY_TOP = Color(0xFF14151E)
private val DAY_BOTTOM = Color(0xFF1D1C29)
