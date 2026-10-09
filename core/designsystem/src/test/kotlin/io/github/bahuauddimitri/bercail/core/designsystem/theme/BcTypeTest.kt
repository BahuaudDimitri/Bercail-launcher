package io.github.bahuauddimitri.bercail.core.designsystem.theme

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.prop
import org.junit.Test

class BcTypeTest {
    @Test
    fun `aucun texte n'est plus petit que 10,5 sp`() {
        assertThat(BcType.all).isNotEmpty()
        BcType.all.values.forEach { style ->
            assertThat(style.fontSize.value).isGreaterThanOrEqualTo(MIN_TEXT_SP)
        }
    }

    @Test
    fun `le titre du rendez-vous est en grand et léger`() {
        assertThat(BcType.agendaTitle).all {
            prop("taille") { it.fontSize }.isEqualTo(30.sp)
            prop("graisse") { it.fontWeight }.isEqualTo(FontWeight.Light)
        }
    }

    @Test
    fun `tous les textes utilisent la police Outfit`() {
        BcType.all.values.forEach { style ->
            assertThat(style.fontFamily).isEqualTo(Outfit)
        }
    }

    private companion object {
        const val MIN_TEXT_SP = 10.5f
    }
}
