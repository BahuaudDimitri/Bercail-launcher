package io.github.bahuauddimitri.bercail.core.domain.apps

import io.github.bahuauddimitri.bercail.core.domain.Picture

/**
 * An icon pack installed on the phone: an app answering the ADW/Nova standard, which draws other apps' icons. [id]
 * is its package; [name], the name of the app.
 */
data class IconPack(val id: String, val name: String)

/** The pack the dock draws with: the chosen one while it is installed; otherwise none, the dock's own glyphs. */
fun iconPackShown(chosen: String?, installed: List<IconPack>): IconPack? = installed.firstOrNull { it.id == chosen }

/**
 * A drawing of an icon pack as a white glyph, for the dock. Light line art (Lines, Whicons…) keeps its strokes,
 * each as opaque as it is light, so that the dark shadow under them goes; any other drawing is drawn in one colour
 * as an app's icon is ([iconGlyph]). Nothing when it is empty.
 */
fun packGlyph(drawing: Picture): Picture? {
    val seen = drawing.pixels.filter { it.alpha >= SEEN_ALPHA }
    if (seen.isEmpty()) return null
    val lightLines = seen.sumOf { it.lightness } / seen.size >= MIN_LINE_LIGHTNESS
    return if (lightLines) {
        val pixels = IntArray(drawing.pixels.size) { index ->
            val pixel = drawing.pixels[index]
            (pixel.alpha * pixel.lightness / CHANNEL shl ALPHA_SHIFT) or WHITE
        }
        Picture(drawing.width, drawing.height, pixels)
    } else {
        iconGlyph(drawing)
    }
}

private val Int.alpha: Int get() = ushr(ALPHA_SHIFT)

/** How light a pixel is: its brightest channel, as a white stroke and a red one are both drawn strokes. */
private val Int.lightness: Int
    get() = maxOf((this shr RED_SHIFT) and CHANNEL, (this shr GREEN_SHIFT) and CHANNEL, this and CHANNEL)

/** Strokes this light on average, out of 255, make line art meant to be shown light. */
private const val MIN_LINE_LIGHTNESS = 160

/** Pixels fainter than half opaque (soft edges, a shadow) do not tell what the drawing is. */
private const val SEEN_ALPHA = 128
private const val ALPHA_SHIFT = 24
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val CHANNEL = 0xFF
private const val WHITE = 0x00FFFFFF
