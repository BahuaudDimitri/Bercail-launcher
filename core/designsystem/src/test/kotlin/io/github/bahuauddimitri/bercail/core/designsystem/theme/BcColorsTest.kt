package io.github.bahuauddimitri.bercail.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isLessThan
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextColor
import org.junit.Test

class BcColorsTest {
    @Test
    fun `le texte principal est lisible sur tout le fond de nuit`() {
        BcColors.backgrounds.forEach { background ->
            assertThat(contrastRatio(BcColors.text, background)).isGreaterThanOrEqualTo(WCAG_TEXT)
        }
    }

    @Test
    fun `le texte discret est lisible sur tout le fond de nuit`() {
        BcColors.backgrounds.forEach { background ->
            assertThat(contrastRatio(BcColors.textMuted, background)).isGreaterThanOrEqualTo(WCAG_TEXT)
        }
    }

    @Test
    fun `chaque pastel se distingue du fond`() {
        BcColors.pastels.forEach { pastel ->
            assertThat(contrastRatio(pastel, BcColors.base)).isGreaterThanOrEqualTo(WCAG_SHAPE)
        }
    }

    @Test
    fun `le texte posé sur un pastel est lisible`() {
        BcColors.pastels.forEach { pastel ->
            assertThat(contrastRatio(BcColors.onPastel, pastel)).isGreaterThanOrEqualTo(WCAG_TEXT)
        }
    }

    @Test
    fun `le texte posé sur la surface claire est lisible`() {
        assertThat(contrastRatio(BcColors.onLight, BcColors.text)).isGreaterThanOrEqualTo(WCAG_TEXT)
    }

    @Test
    fun `une commande éteinte reste discrète à côté des pastels`() {
        BcColors.pastels.forEach { pastel ->
            assertThat(BcColors.off.luminanceRatioTo(pastel)).isLessThan(1f)
        }
    }

    @Test
    fun `chaque moment a son accent, pêche le matin, bleu-vert la journée, lavande le soir`() {
        assertThat(BcMoment.Morning.accent).isEqualTo(Color(0xFFF6C28B))
        assertThat(BcMoment.Day.accent).isEqualTo(Color(0xFF9FD8C4))
        assertThat(BcMoment.Evening.accent).isEqualTo(Color(0xFFB4A7F0))
    }

    @Test
    fun `l'accent de chaque moment se distingue du fond de nuit`() {
        BcMoment.entries.forEach { moment ->
            BcColors.backgrounds.forEach { background ->
                assertThat(contrastRatio(moment.accent, background)).isGreaterThanOrEqualTo(WCAG_SHAPE)
            }
        }
    }

    @Test
    fun `le halo est l'accent à 55 %`() {
        BcMoment.entries.forEach { moment ->
            assertThat(moment.halo).isEqualTo(moment.accent.copy(alpha = 0.55f))
        }
    }

    @Test
    fun `les traits sont blancs à 22 % pour le fil, 35 % pour les anneaux, 20 % pour les boutons`() {
        assertThat(BcColors.stroke).isEqualTo(BcColors.text.copy(alpha = 0.22f))
        assertThat(BcColors.strokeRing).isEqualTo(BcColors.text.copy(alpha = 0.35f))
        assertThat(BcColors.strokeButton).isEqualTo(BcColors.text.copy(alpha = 0.2f))
    }

    @Test
    fun `le fond de base, sous la brume et derrière les réglages, est le noir bleuté #101119`() {
        assertThat(BcColors.base).isEqualTo(Color(0xFF101119))
    }

    @Test
    fun `les filets entre les réglages sont blancs à 10 %, le bord d'un sélecteur blanc à 12 %`() {
        assertThat(BcColors.hairline).isEqualTo(BcColors.text.copy(alpha = 0.1f))
        assertThat(BcColors.segmentEdge).isEqualTo(Color.White.copy(alpha = 0.12f))
    }

    @Test
    fun `le chevron du bas d'Écoute est du texte à 45 %, plus pâle que le texte discret`() {
        assertThat(BcColors.textFaint).isEqualTo(BcColors.text.copy(alpha = 0.45f))
        assertThat(BcTextColor.Faint.color).isEqualTo(BcColors.textFaint)
    }

    @Test
    fun `le fond d'un bouton bordé est blanc à 6 %`() {
        assertThat(BcColors.buttonFill).isEqualTo(BcColors.text.copy(alpha = 0.06f))
    }

    @Test
    fun `le texte d'un bouton plein d'accent est lisible sur l'accent de chaque moment`() {
        BcMoment.entries.forEach { moment ->
            assertThat(contrastRatio(BcColors.onAccent, moment.accent)).isGreaterThanOrEqualTo(WCAG_TEXT)
        }
    }

    @Test
    fun `le contraste est calculé selon la formule WCAG`() {
        assertThat(contrastRatio(Color.White, Color.Black)).isCloseTo(21f, 0.01f)
        assertThat(contrastRatio(Color.Black, Color.Black)).isCloseTo(1f, 0.01f)
    }

    private fun Color.luminanceRatioTo(other: Color) = (luminance() + 0.05f) / (other.luminance() + 0.05f)

    private companion object {
        /** WCAG AA for body text. */
        const val WCAG_TEXT = 4.5f

        /** WCAG AA for shapes and icons. */
        const val WCAG_SHAPE = 3f
    }
}
