package io.github.bahuauddimitri.bercail.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isLessThan
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
    fun `une commande éteinte reste discrète à côté des pastels`() {
        BcColors.pastels.forEach { pastel ->
            assertThat(BcColors.off.luminanceRatioTo(pastel)).isLessThan(1f)
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
