package io.github.bahuauddimitri.bercail.core.domain.apps

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import io.github.bahuauddimitri.bercail.core.domain.Picture
import org.junit.Test

/** The icon packs of the dock: which one applies, and how its drawings become white line art. */
class IconPackTest {
    private val lines = IconPack(id = "com.natewren.linesfree", name = "Lines Free")
    private val whicons = IconPack(id = "com.whicons.iconpack", name = "Whicons")

    /** A 10 × 10 picture: the first [strokes] pixels in [stroke], the next [shadows] in [shadow], the rest clear. */
    private fun drawing(strokes: Int, stroke: Int, shadows: Int = 0, shadow: Int = 0): Picture {
        val pixels = IntArray(100)
        pixels.fill(stroke, toIndex = strokes)
        pixels.fill(shadow, fromIndex = strokes, toIndex = strokes + shadows)
        return Picture(10, 10, pixels)
    }

    @Test
    fun `le pack choisi s'applique tant qu'il est installé`() {
        assertThat(iconPackShown(lines.id, listOf(whicons, lines))).isEqualTo(lines)
    }

    @Test
    fun `un pack désinstallé revient à Aucun sans rien dire`() {
        assertThat(iconPackShown(lines.id, listOf(whicons))).isNull()
    }

    @Test
    fun `sans pack choisi, Aucun reste Aucun`() {
        assertThat(iconPackShown(null, listOf(whicons, lines))).isNull()
    }

    @Test
    fun `un pack aux traits clairs garde ses traits en blanc et perd son ombre sombre`() {
        val glyph = packGlyph(drawing(strokes = 20, stroke = 0xFFFFFFFF.toInt(), shadows = 10, shadow = 0x60000000))

        assertThat(glyph).isNotNull()
        assertThat(glyph!!.pixels.take(20)).containsOnly(0xFFFFFFFF.toInt())
        assertThat(glyph.pixels.drop(20)).containsOnly(0x00FFFFFF)
    }

    @Test
    fun `un pack aux traits sombres est dessiné en blanc, sa forme gardée`() {
        val glyph = packGlyph(drawing(strokes = 20, stroke = 0xFF202020.toInt()))

        assertThat(glyph).isNotNull()
        assertThat(glyph!!.pixels.take(20)).containsOnly(0xFFFFFFFF.toInt())
        assertThat(glyph.pixels.drop(20)).containsOnly(0x00FFFFFF)
    }

    @Test
    fun `un dessin vide du pack ne donne rien`() {
        assertThat(packGlyph(drawing(strokes = 0, stroke = 0))).isNull()
    }
}
