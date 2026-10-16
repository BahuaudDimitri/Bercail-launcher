package io.github.bahuauddimitri.bercail.core.domain.apps

import io.github.bahuauddimitri.bercail.core.domain.Picture

/**
 * The monochrome layer an app gives for themed icons, as a white glyph: only its shape counts, carried by the
 * alpha; the screen tints it.
 */
fun monochromeGlyph(layer: Picture): Picture = Picture(layer.width, layer.height, layer.pixels.whitened())

/**
 * The silhouette of an icon without monochrome layer, as a white glyph, when it reads as a drawing: a logo on a
 * transparent ground. A shape that fills most of the icon (a full disc, a square, an old icon without layers) says
 * nothing once in one colour, and nothing at all is no drawing either: the screen then shows the initial.
 */
fun silhouetteGlyph(icon: Picture): Picture? {
    val seen = icon.pixels.count { it.alpha >= SEEN_ALPHA }
    val coverage = seen.toFloat() / icon.pixels.size
    if (seen == 0 || coverage > MAX_SILHOUETTE_COVERAGE) return null
    return Picture(icon.width, icon.height, icon.pixels.whitened())
}

private fun IntArray.whitened(): IntArray = IntArray(size) { (this[it] and ALPHA_MASK) or WHITE }

private val Int.alpha: Int get() = ushr(ALPHA_SHIFT)

/** Past half the icon, a silhouette is a blot rather than a drawing. */
private const val MAX_SILHOUETTE_COVERAGE = 0.5f

/** Pixels fainter than half opaque (soft edges, a faint wash) do not make the shape. */
private const val SEEN_ALPHA = 128
private const val ALPHA_SHIFT = 24
private const val ALPHA_MASK = -0x1000000
private const val WHITE = 0x00FFFFFF
