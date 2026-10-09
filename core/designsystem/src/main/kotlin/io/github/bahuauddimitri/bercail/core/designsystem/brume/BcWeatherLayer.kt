@file:Suppress("MagicNumber") // Particle counts, speeds and sizes copied from the prototype.

package io.github.bahuauddimitri.bercail.core.designsystem.brume

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import kotlin.random.Random

/*
 * Weather particles drawn over Brume. Every position is a pure function of time: nothing is stored between frames,
 * so a still Brume shows a still sky and a test always sees the same picture.
 */

private class Star(val x: Float, val y: Float, val brightness: Float)

private class Drop(val x: Float, val y: Float, val speed: Float)

private class Gust(val x: Float, val y: Float, val length: Float, val speed: Float, val phase: Float)

private class Flake(val x: Float, val y: Float, val radius: Float, val speed: Float, val phase: Float)

private val STARS = Random(7).let { r -> List(60) { Star(r.nextFloat(), r.nextFloat(), r.nextFloat()) } }
private val DROPS = Random(11).let { r -> List(70) { Drop(r.nextFloat(), r.nextFloat(), 0.6f + r.nextFloat() * 0.6f) } }
private val GUSTS = Random(13).let { r ->
    List(7) {
        Gust(
            r.nextFloat(),
            0.12f + r.nextFloat() * 0.7f,
            0.25f + r.nextFloat() * 0.3f,
            0.25f + r.nextFloat() * 0.2f,
            r.nextFloat() * 6
        )
    }
}
private val FLAKES = Random(17).let { r ->
    List(90) {
        Flake(
            r.nextFloat(),
            r.nextFloat(),
            0.6f + r.nextFloat() * 1.9f,
            0.04f + r.nextFloat() * 0.06f,
            r.nextFloat() * 6
        )
    }
}

/** Wraps [value] into [start, start + span). */
private fun wrap(value: Float, start: Float, span: Float) = start + ((value - start) % span + span) % span

internal fun DrawScope.drawStars(t: Float) {
    STARS.forEachIndexed { index, star ->
        val twinkle = if (index % 7 == 0) 0.6f + 0.4f * sin(t * 2 + index) else 1f
        drawCircle(
            color = Color.White.copy(alpha = ((0.15f + star.brightness * 0.45f) * twinkle).coerceIn(0f, 1f)),
            radius = (0.4f + star.brightness * 0.9f).dp.toPx(),
            center = Offset(star.x * size.width, star.y * size.height * 0.75f)
        )
    }
}

internal fun DrawScope.drawRain(t: Float) {
    val color = Color(0xFFDCE6FF).copy(alpha = 0.22f)
    val slant = 3.dp.toPx()
    val fall = 13.dp.toPx()
    val width = 1.dp.toPx()
    DROPS.forEach { drop ->
        val y = wrap(drop.y + drop.speed * t * 0.9f, -0.05f, 1.1f) * size.height
        val x = drop.x * size.width
        drawLine(color, Offset(x, y), Offset(x - slant, y + fall), width)
    }
}

internal fun DrawScope.drawGusts(t: Float) {
    val step = 6.dp.toPx()
    val wave = 5.dp.toPx()
    GUSTS.forEach { gust ->
        val head = wrap(gust.x + gust.speed * t, -0.05f, 1.15f + gust.length)
        val x1 = (head - gust.length) * size.width
        val x2 = head * size.width
        if (x2 <= x1) return@forEach
        val y0 = gust.y * size.height
        val path = Path()
        var x = x1
        path.moveTo(x, y0 + wave * sin(x / 38f + t * 2.2f + gust.phase))
        while (x < x2) {
            x += step
            path.lineTo(x, y0 + wave * sin(x / 38f + t * 2.2f + gust.phase))
        }
        drawPath(
            path = path,
            brush = Brush.horizontalGradient(
                0f to Color.White.copy(alpha = 0f),
                0.7f to Color.White.copy(alpha = 0.34f),
                1f to Color.White.copy(alpha = 0f),
                startX = x1,
                endX = x2
            ),
            style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

internal fun DrawScope.drawSnow(t: Float) {
    FLAKES.forEach { flake ->
        val y = wrap(flake.y + flake.speed * t * (1 + flake.radius * 0.3f), -0.02f, 1.04f) * size.height
        val x = (flake.x + 0.012f * sin(t * 0.8f + flake.phase)) * size.width
        drawCircle(
            color = Color.White.copy(alpha = (0.35f + flake.radius * 0.2f).coerceIn(0f, 1f)),
            radius = flake.radius.dp.toPx(),
            center = Offset(x, y)
        )
    }
}
