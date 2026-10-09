package io.github.bahuauddimitri.bercail.core.designsystem.brume

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThanOrEqualTo
import org.junit.Test

class BcBrumeColorsTest {
    @Test
    fun `les couleurs de Brume restent sombres même avec une pochette blanche`() {
        val colors = BcBrumeColors.of(Color.White, Color(0xFFFFF59D))

        assertThat(colors.first.luminance()).isLessThanOrEqualTo(MAX_LUMINANCE + EPSILON)
        assertThat(colors.second.luminance()).isLessThanOrEqualTo(MAX_LUMINANCE + EPSILON)
    }

    @Test
    fun `une pochette déjà sombre garde ses couleurs`() {
        val deepBlue = Color(0xFF1E3A8A)

        assertThat(BcBrumeColors.of(deepBlue, deepBlue).first).isEqualTo(deepBlue)
    }

    @Test
    fun `les couleurs du moment restent sombres`() {
        listOf(BcBrumeColors.Morning, BcBrumeColors.Day, BcBrumeColors.Evening).forEach { moment ->
            assertThat(moment.first.luminance()).isLessThanOrEqualTo(MAX_LUMINANCE + EPSILON)
            assertThat(moment.second.luminance()).isLessThanOrEqualTo(MAX_LUMINANCE + EPSILON)
        }
    }

    private companion object {
        const val EPSILON = 0.01f
    }
}
