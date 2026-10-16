package io.github.bahuauddimitri.bercail.core.domain.apps

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import io.github.bahuauddimitri.bercail.core.domain.Picture
import org.junit.Test

/**
 * The icon of an app drawn in one colour: its monochrome layer, or else the real icon itself, what stands out
 * from its ground.
 */
class GlyphTest {
    private val red = 0xFFE53935.toInt()
    private val navy = 0xFF000080.toInt()
    private val white = 0xFFFFFFFF.toInt()
    private val clear = 0x00000000

    /** A 10 × 10 picture whose first [filled] pixels are [color], the others [rest]. */
    private fun picture(filled: Int, color: Int = red, rest: Int = clear): Picture {
        val pixels = IntArray(100) { rest }
        pixels.fill(color, toIndex = filled)
        return Picture(10, 10, pixels)
    }

    private val Int.alpha: Int get() = ushr(24)

    @Test
    fun `un dessin monochrome devient blanc et garde sa forme`() {
        val glyph = monochromeGlyph(picture(filled = 30, color = 0x80123456.toInt()))

        assertThat(glyph.pixels.take(30)).containsOnly(0x80FFFFFF.toInt())
        assertThat(glyph.pixels.drop(30)).containsOnly(0x00FFFFFF)
        assertThat(glyph.width).isEqualTo(10)
    }

    @Test
    fun `une icône pleine est rendue en une seule couleur, ce qui se détache de son fond`() {
        val glyph = iconGlyph(picture(filled = 20, color = white, rest = navy))

        assertThat(glyph).isNotNull()
        assertThat(glyph!!.pixels.take(20)).containsOnly(white)
        assertThat(glyph.pixels.drop(20)).containsOnly(0x00FFFFFF)
    }

    @Test
    fun `le dessin d'une icône pleine garde sa forme, ses coins transparents le restent`() {
        val pixels = IntArray(100) { if (it < 20) white else navy }
        pixels.fill(clear, fromIndex = 90)

        val glyph = iconGlyph(Picture(10, 10, pixels))!!

        assertThat(glyph.pixels.drop(90).map { it.alpha }).containsOnly(0)
    }

    @Test
    fun `un fond en léger dégradé ne se voit pas dans le dessin`() {
        val pixels = IntArray(100) { if (it < 20) white else navy + (it % 3) * 0x000008 }

        val glyph = iconGlyph(Picture(10, 10, pixels))!!

        assertThat(glyph.pixels.drop(20).map { it.alpha }).containsOnly(0)
    }

    @Test
    fun `les bords adoucis du dessin restent adoucis`() {
        val pixels = IntArray(100) { if (it < 20) white else navy }
        pixels[20] = 0xFF808080.toInt()

        val glyph = iconGlyph(Picture(10, 10, pixels))!!

        assertThat(glyph.pixels[20].alpha).isBetween(1, 254)
    }

    @Test
    fun `une icône unie, où rien ne se détache, ne fait pas de dessin`() {
        assertThat(iconGlyph(picture(filled = 100, color = navy))).isNull()
    }

    @Test
    fun `un dessin sur un fond transparent garde sa forme`() {
        val glyph = iconGlyph(picture(filled = 30))

        assertThat(glyph).isNotNull()
        assertThat(glyph!!.pixels.take(30)).containsOnly(white)
        assertThat(glyph.pixels.drop(30).map { it.alpha }).containsOnly(0)
    }

    @Test
    fun `une icône vide ne fait pas de dessin`() {
        assertThat(iconGlyph(picture(filled = 0))).isNull()
    }

    @Test
    fun `les pixels à peine visibles ne comptent pas dans la forme`() {
        val faint = Picture(10, 10, IntArray(100) { if (it < 30) red else 0x10FFFFFF })

        val glyph = iconGlyph(faint)

        assertThat(glyph).isNotNull()
        assertThat(glyph!!.pixels.take(30)).containsOnly(white)
    }
}
