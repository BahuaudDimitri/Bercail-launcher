package io.github.bahuauddimitri.bercail.core.designsystem.theme

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
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
    fun `le titre du rendez-vous est en grand et très léger, 34 sp graisse 200`() {
        assertThat(BcType.agendaTitle).all {
            prop("taille") { it.fontSize }.isEqualTo(34.sp)
            prop("graisse") { it.fontWeight }.isEqualTo(FontWeight.ExtraLight)
        }
    }

    @Test
    fun `le titre d'une ligne de liste est en 17 sp graisse 400, son sous-titre en 12 sp`() {
        assertThat(BcType.listTitle).all {
            prop("taille") { it.fontSize }.isEqualTo(17.sp)
            prop("graisse") { it.fontWeight }.isEqualTo(FontWeight.Normal)
        }
        assertThat(BcType.listSubtitle).all {
            prop("taille") { it.fontSize }.isEqualTo(12.sp)
            prop("graisse") { it.fontWeight }.isEqualTo(FontWeight.Normal)
        }
    }

    @Test
    fun `l'initiale dans l'anneau d'une ligne est en 13,5 sp graisse 600`() {
        assertThat(BcType.listInitial).all {
            prop("taille") { it.fontSize }.isEqualTo(13.5.sp)
            prop("graisse") { it.fontWeight }.isEqualTo(FontWeight.SemiBold)
        }
    }

    @Test
    fun `un titre de section est en 11 sp graisse 600, lettres espacées d'un dixième`() {
        assertThat(BcType.sectionLabel).all {
            prop("taille") { it.fontSize }.isEqualTo(11.sp)
            prop("graisse") { it.fontWeight }.isEqualTo(FontWeight.SemiBold)
            prop("espacement") { it.letterSpacing }.isEqualTo(0.1.em)
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
