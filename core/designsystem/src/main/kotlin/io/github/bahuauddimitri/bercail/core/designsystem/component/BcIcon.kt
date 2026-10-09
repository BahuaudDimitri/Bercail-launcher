@file:Suppress("MagicNumber") // Coordinates copied from the prototype's SVG.

package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSizes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.LocalBcContentColor

/** The prototype's icon set, drawn from its SVG paths. */
enum class BcIcons(private val draw: () -> ImageVector) {
    Lamp({ line24 { stroke("M8 3h8l3 7H5zM12 10v8M8 20.5h8") } }),
    Film({
        line24 {
            stroke(roundRect(3.5f, 5.5f, 17f, 13f, 3f))
            fill("M10 9.5v5l4.5-2.5z")
        }
    }),
    Shutters({
        line24 {
            stroke(roundRect(4.5f, 3.5f, 15f, 17f, 2.5f))
            stroke("M4.5 8.5h15M4.5 12.5h15M4.5 16.5h15")
        }
    }),
    Flame({ line24 { stroke(FLAME) } }),
    Chat({ line24 { stroke(CHAT) } }),
    House({ line24 { stroke("M4 11.5 12 5l8 6.5V19a1 1 0 0 1-1 1h-4.5v-5h-5v5H5a1 1 0 0 1-1-1z") } }),
    Calendar({
        line24 {
            stroke(roundRect(4f, 5.5f, 16f, 14f, 2.5f))
            stroke("M4 10h16M8.5 3.5v4M15.5 3.5v4")
        }
    }),
    Web({ line24 { stroke(circle(12f, 12f, 8f) + WEB) } }),
    Store({ line24 { stroke("M5 8h14l-1 11H6zM9 8a3 3 0 0 1 6 0") } }),
    Settings({
        line24 {
            stroke(circle(12f, 12f, 3f))
            stroke(COG)
        }
    }),
    Note({ line24 { stroke("M9 18V6l10-2v12" + circle(6.5f, 18f, 2.5f) + circle(16.5f, 16f, 2.5f)) } }),
    Play({ glyph(14f, 14f) { fill("M3.5 1.8v10.4L12 7z") } }),
    Pause({ glyph(14f, 14f) { fill("M3 2h3v10H3zM8 2h3v10H8z") } }),
    Previous({ glyph(16f, 16f) { fillAndStroke("M3 3v10M13 3.5 6 8l7 4.5z", width = 1.4f) } }),
    Next({ glyph(16f, 16f) { fillAndStroke("M13 3v10M3 3.5 10 8l-7 4.5z", width = 1.4f) } }),
    Mic({
        glyph(14f, 18f) {
            fill(roundRect(4f, 1f, 6f, 10f, 3f))
            stroke("M1.5 8a5.5 5.5 0 0 0 11 0M7 13.5V17", width = 1.6f)
        }
    }),
    Send({ glyph(16f, 16f) { stroke("M3 8h10M9 4l4 4-4 4", width = 1.8f) } }),
    Back({ glyph(18f, 18f) { stroke("M11 3 5 9l6 6", width = 1.8f) } }),
    Search({ glyph(16f, 16f) { stroke(circle(7f, 7f, 5f) + "M11 11l3.5 3.5", width = 1.6f) } }),
    Close({ glyph(14f, 14f) { stroke("M3 3l8 8M11 3l-8 8", width = 1.8f) } })
    ;

    internal val vector: ImageVector by lazy { draw() }
}

/** An icon of the set, tinted with the color of the surface it sits on unless [tint] says otherwise. */
@Composable
fun BcIcon(
    icon: BcIcons,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = BcSizes.icon,
    tint: BcTextColor? = null
) {
    Image(
        painter = rememberVectorPainter(icon.vector),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(tint?.color ?: LocalBcContentColor.current)
    )
}

private const val FLAME =
    "M12 3c3 4 5.5 6.2 5.5 10.2a5.5 5.5 0 0 1-11 0c0-2.2 1.1-3.8 2.2-4.8 0 2 1 3.2 2.1 3.2 0-3.1-.9-5.4 1.2-8.6z"

private const val CHAT =
    "M5 5h14a1.5 1.5 0 0 1 1.5 1.5v8A1.5 1.5 0 0 1 19 16h-8l-4.5 3.5V16H5a1.5 1.5 0 0 1-1.5-1.5v-8" +
        "A1.5 1.5 0 0 1 5 5z"

private const val WEB = "M4 12h16M12 4c2.5 2.5 2.5 13.5 0 16M12 4c-2.5 2.5-2.5 13.5 0 16"

private const val COG =
    "M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 " +
        "1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33" +
        "l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3" +
        "a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83" +
        "l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 " +
        "1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9" +
        "a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"

private const val LINE_WIDTH = 1.7f
private val Ink = SolidColor(Color.Black)

private fun line24(block: ImageVector.Builder.() -> Unit) = glyph(24f, 24f, block)

private fun glyph(width: Float, height: Float, block: ImageVector.Builder.() -> Unit) = ImageVector.Builder(
    defaultWidth = width.dp,
    defaultHeight = height.dp,
    viewportWidth = width,
    viewportHeight = height
).apply(block).build()

private fun ImageVector.Builder.stroke(path: String, width: Float = LINE_WIDTH) = addPath(
    pathData = addPathNodes(path),
    stroke = Ink,
    strokeLineWidth = width,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round
)

private fun ImageVector.Builder.fill(path: String) = addPath(pathData = addPathNodes(path), fill = Ink)

private fun ImageVector.Builder.fillAndStroke(path: String, width: Float) = addPath(
    pathData = addPathNodes(path),
    fill = Ink,
    stroke = Ink,
    strokeLineWidth = width,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round
)

/** SVG <circle> as a path. */
private fun circle(cx: Float, cy: Float, r: Float) = "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

/** SVG <rect rx> as a path. */
private fun roundRect(x: Float, y: Float, w: Float, h: Float, r: Float): String {
    val inner = w - 2 * r
    val innerH = h - 2 * r
    return "M${x + r} ${y}h${inner}a$r $r 0 0 1 $r ${r}v${innerH}a$r $r 0 0 1 ${-r} ${r}h${-inner}" +
        "a$r $r 0 0 1 ${-r} ${-r}v${-innerH}a$r $r 0 0 1 $r ${-r}z"
}
