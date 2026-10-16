package io.github.bahuauddimitri.bercail.core.domain.apps

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import io.github.bahuauddimitri.bercail.core.domain.Picture
import org.junit.Test

/** The icon of an app drawn as a one-colour glyph: its monochrome layer, or its silhouette when it reads. */
class GlyphTest {
    private val red = 0xFFE53935.toInt()
    private val clear = 0x00000000

    /** A 10 × 10 picture whose first [filled] pixels are [color], the others transparent. */
    private fun picture(filled: Int, color: Int = red): Picture {
        val pixels = IntArray(100) { clear }
        pixels.fill(color, toIndex = filled)
        return Picture(10, 10, pixels)
    }

    @Test
    fun `un dessin monochrome devient blanc et garde sa forme`() {
        val glyph = monochromeGlyph(picture(filled = 30, color = 0x80123456.toInt()))

        assertThat(glyph.pixels.take(30)).containsOnly(0x80FFFFFF.toInt())
        assertThat(glyph.pixels.drop(30)).containsOnly(0x00FFFFFF)
        assertThat(glyph.width).isEqualTo(10)
    }

    @Test
    fun `une silhouette fine devient un dessin d'une seule couleur`() {
        val glyph = silhouetteGlyph(picture(filled = 30))

        assertThat(glyph).isNotNull()
        assertThat(glyph!!.pixels.take(30)).containsOnly(0xFFFFFFFF.toInt())
    }

    @Test
    fun `une silhouette qui remplit presque toute l'icône ne fait pas un dessin`() {
        assertThat(silhouetteGlyph(picture(filled = 70))).isNull()
    }

    @Test
    fun `une icône vide ne fait pas un dessin`() {
        assertThat(silhouetteGlyph(picture(filled = 0))).isNull()
    }

    @Test
    fun `les pixels à peine visibles ne comptent pas dans la silhouette`() {
        val faint = Picture(10, 10, IntArray(100) { if (it < 30) red else 0x10FFFFFF })

        assertThat(silhouetteGlyph(faint)).isNotNull()
    }
}
