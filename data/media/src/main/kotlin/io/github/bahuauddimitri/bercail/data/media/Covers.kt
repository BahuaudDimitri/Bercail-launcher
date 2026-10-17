package io.github.bahuauddimitri.bercail.data.media

import android.graphics.Bitmap
import androidx.palette.graphics.Palette
import io.github.bahuauddimitri.bercail.core.domain.Picture
import io.github.bahuauddimitri.bercail.core.domain.media.CoverColors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** A cover as Bercail keeps it: its picture, and the two colors that tint Brume. */
internal class Cover(val picture: Picture, val colors: CoverColors?)

/**
 * Reads the cover an app gives. A larger one than the arch of the Listen screen shows (a video thumbnail, a cover in
 * full resolution) is reduced first, to [LONGEST_SIDE]; a smaller one is kept as it is, never enlarged.
 * The pixels are copied, because the picture of the app lives in memory Bercail does not own.
 */
internal fun Bitmap.asCover(): Cover {
    val readable = if (config == Bitmap.Config.HARDWARE) copy(Bitmap.Config.ARGB_8888, false) else this
    val scale = min(1f, LONGEST_SIDE.toFloat() / max(width, height))
    val sized = if (scale < 1f) {
        Bitmap.createScaledBitmap(readable, (width * scale).roundToInt(), (height * scale).roundToInt(), true)
    } else {
        readable
    }
    val pixels = IntArray(sized.width * sized.height)
    sized.getPixels(pixels, 0, sized.width, 0, 0, sized.width, sized.height)
    val cover = Cover(Picture(sized.width, sized.height, pixels), sized.twoColors())
    // Only what was made here is thrown away: the picture of the app stays its own.
    if (sized !== readable) sized.recycle()
    if (readable !== this) readable.recycle()
    return cover
}

/**
 * The most present color of the cover, then the most present of those that stand apart from it. Near-black and
 * near-white do not count: they tint nothing. Every other color does, oranges included, which Palette leaves
 * out by default because they look like skin. The colors are read on a copy of about 112 × 112 px, whatever the
 * size of the cover: a large cover costs barely more to read than a small one.
 */
private fun Bitmap.twoColors(): CoverColors? {
    val colors = Palette.from(this)
        .resizeBitmapArea(SAMPLE_SIDE * SAMPLE_SIDE)
        .clearFilters()
        .addFilter { _, hsl -> hsl[2] in DARKEST..LIGHTEST }
        .generate()
        .swatches
        .sortedByDescending { it.population }
    val main = colors.firstOrNull() ?: return null
    val other = colors.firstOrNull { it.standsApartFrom(main) } ?: main
    return CoverColors(main.rgb, other.rgb)
}

/** Two colors stand apart when the eye tells them at once: another hue, or clearly lighter, darker or duller. */
private fun Palette.Swatch.standsApartFrom(other: Palette.Swatch): Boolean {
    val (hue, saturation, lightness) = hsl
    val (otherHue, otherSaturation, otherLightness) = other.hsl
    val turn = abs(hue - otherHue).let { min(it, FULL_TURN - it) }
    // The hue of a grey means nothing.
    val colored = saturation > GREY && otherSaturation > GREY
    return (colored && turn >= HUE_APART) ||
        abs(lightness - otherLightness) >= LIGHTNESS_APART ||
        abs(saturation - otherSaturation) >= SATURATION_APART
}

/**
 * The longest side a cover is kept at: the arch of the Listen screen shows it 484 dp tall, about 1 270 px on the
 * Pixel 9 (2.625 px per dp), rounded up. A smaller cover is kept as it is: it is never enlarged here. A square cover
 * of this size weighs 6.5 MB; Bercail keeps one for each media app whose session is open.
 */
private const val LONGEST_SIDE = 1280
private const val SAMPLE_SIDE = 112
private const val FULL_TURN = 360f
private const val HUE_APART = 30f
private const val LIGHTNESS_APART = 0.25f
private const val SATURATION_APART = 0.4f
private const val GREY = 0.1f
private const val DARKEST = 0.05f
private const val LIGHTEST = 0.95f
