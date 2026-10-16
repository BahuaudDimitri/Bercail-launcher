package io.github.bahuauddimitri.bercail.core.domain.apps

import io.github.bahuauddimitri.bercail.core.domain.Picture
import kotlin.math.abs

/**
 * The monochrome layer an app gives for themed icons, as a white glyph: only its shape counts, carried by the
 * alpha; the screen tints it.
 */
fun monochromeGlyph(layer: Picture): Picture = Picture(layer.width, layer.height, layer.pixels.whitened())

/**
 * The real icon of an app without monochrome layer, in one colour, as a white glyph. A full icon (a logo on its
 * ground, as most icons are) keeps what stands out from its ground, the more it stands out the more opaque, so that
 * the logo reads and the ground disappears; a drawing on a transparent ground keeps its shape. Nothing when the icon
 * is empty or plain: the screen then shows the initial.
 */
fun iconGlyph(icon: Picture): Picture? {
    val seen = icon.pixels.count { it.alpha >= SEEN_ALPHA }
    return when {
        seen == 0 -> null
        seen.toFloat() / icon.pixels.size <= MAX_DRAWING_COVERAGE -> monochromeGlyph(icon)
        else -> standingOut(icon)
    }
}

/** What stands out from the ground of a full icon, as a white glyph; nothing when nothing does. */
private fun standingOut(icon: Picture): Picture? {
    val seen = icon.pixels.filter { it.alpha >= SEEN_ALPHA }
    val ground = Rgb(median(seen) { it.red }, median(seen) { it.green }, median(seen) { it.blue })
    val distances = IntArray(icon.pixels.size) { ground.distanceTo(icon.pixels[it]) }
    val strongest = seen.maxOf { ground.distanceTo(it) }
    if (strongest < MIN_CONTRAST) return null
    val noise = strongest * GROUND_NOISE
    val pixels = IntArray(icon.pixels.size) { index ->
        val strength = ((distances[index] - noise) / (strongest - noise)).coerceIn(0f, 1f)
        val alpha = (icon.pixels[index].alpha * strength).toInt()
        (alpha shl ALPHA_SHIFT) or WHITE
    }
    return Picture(icon.width, icon.height, pixels)
}

/** The colour of the ground of an icon. */
private class Rgb(val red: Int, val green: Int, val blue: Int) {
    /** How far a pixel stands from this colour: its channel that differs the most. */
    fun distanceTo(pixel: Int): Int = maxOf(abs(pixel.red - red), abs(pixel.green - green), abs(pixel.blue - blue))
}

/** The middle value of one channel over [pixels]: the colour most of a full icon is painted in. */
private fun median(pixels: List<Int>, channel: (Int) -> Int): Int = pixels.map(channel).sorted()[pixels.size / 2]

private fun IntArray.whitened(): IntArray = IntArray(size) { (this[it] and ALPHA_MASK) or WHITE }

private val Int.alpha: Int get() = ushr(ALPHA_SHIFT)
private val Int.red: Int get() = (this shr RED_SHIFT) and CHANNEL
private val Int.green: Int get() = (this shr GREEN_SHIFT) and CHANNEL
private val Int.blue: Int get() = this and CHANNEL

/** Up to half the icon covered, an icon on a transparent ground is a drawing: its shape is the glyph. */
private const val MAX_DRAWING_COVERAGE = 0.5f

/** A full icon whose colours stay this close to its ground is plain: no logo to draw. */
private const val MIN_CONTRAST = 48

/** The part of the strongest contrast a slight gradient of the ground may reach without showing. */
private const val GROUND_NOISE = 0.25f

/** Pixels fainter than half opaque (soft edges, a faint wash) do not make the shape. */
private const val SEEN_ALPHA = 128
private const val ALPHA_SHIFT = 24
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val CHANNEL = 0xFF
private const val ALPHA_MASK = -0x1000000
private const val WHITE = 0x00FFFFFF
