package io.github.bahuauddimitri.bercail.feature.home

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isLessThan
import assertk.assertions.isLessThanOrEqualTo
import org.junit.Test

/** Where the Listen screen puts each thing, from the size of the screen: the Pixel 9 as reference, smaller ones fit. */
class ListenLayoutTest {
    private fun Dp.near(expected: Dp) = assertThat(value).isCloseTo(expected.value, TOLERANCE)

    @Test
    fun `sur le Pixel 9, tout est placé comme sur la référence`() {
        val layout = listenLayout(width = 410.67.dp, height = 922.67.dp)

        assertThat(layout.archWidth).isEqualTo(300.dp)
        assertThat(layout.archHeight).isEqualTo(484.dp)
        assertThat(layout.arcRadius).isEqualTo(164.dp)
        assertThat(layout.curveCentre).isEqualTo(334.dp)
        assertThat(layout.titlesTop).isEqualTo(550.dp)
        assertThat(layout.remoteTop).isEqualTo(648.dp)
        assertThat(layout.openTop).isEqualTo(746.dp)
        assertThat(layout.chevronTop).isEqualTo(820.dp)
    }

    @Test
    fun `un écran plus haut que le Pixel 9 ne change rien`() {
        assertThat(listenLayout(width = 480.dp, height = 1100.dp)).isEqualTo(listenLayout(411.dp, 923.dp))
    }

    @Test
    fun `sur un écran moins haut, l'arche perd d'abord sa partie droite et garde son bas en demi-cercle`() {
        val layout = listenLayout(width = 411.dp, height = 780.dp)

        assertThat(layout.archWidth).isEqualTo(300.dp)
        (layout.archHeight - layout.curveCentre).near(150.dp)
        assertThat(layout.archHeight).isLessThan(484.dp)
        assertThat(layout.chevronBottom).isLessThanOrEqualTo(780.dp - MIN_ROOM)
    }

    @Test
    fun `sur un écran moins haut, les écarts entre titre, télécommande, bouton et chevron se resserrent`() {
        val reference = listenLayout(width = 411.dp, height = 923.dp)
        val shorter = listenLayout(width = 411.dp, height = 780.dp)

        assertThat(shorter.remoteTop - shorter.titlesTop).isLessThan(reference.remoteTop - reference.titlesTop)
        assertThat(shorter.openTop - shorter.remoteTop).isLessThan(reference.openTop - reference.remoteTop)
        assertThat(shorter.chevronTop - shorter.openTop).isLessThan(reference.chevronTop - reference.openTop)
    }

    @Test
    fun `sur un écran plus étroit, l'arche rétrécit en gardant ses proportions et l'arc reste autour de sa courbe`() {
        val layout = listenLayout(width = 320.dp, height = 2000.dp)

        // The arc and its 40 dp band fit with 16 dp on each side.
        (layout.arcRadius * 2 + 40.dp).near(320.dp - 32.dp)
        val scale = layout.archWidth / 300.dp
        layout.archHeight.near(484.dp * scale)
        layout.arcRadius.near(164.dp * scale)
        layout.curveCentre.near(layout.archHeight - layout.archWidth / 2)
    }

    @Test
    fun `sur chaque petit écran, tout tient de haut en bas, dans l'ordre, et le chevron reste au-dessus de la barre`() {
        SIZES.forEach { (width, height) ->
            val layout = listenLayout(width, height, bottomInset = 24.dp)
            val timesTop = layout.curveCentre + layout.arcRadius + 16.dp
            assertThat(layout.titlesTop - timesTop).isGreaterThanOrEqualTo(MIN_TIMES)
            assertThat(layout.remoteTop - layout.titlesTop).isGreaterThanOrEqualTo(MIN_TITLES)
            assertThat(layout.openTop - layout.remoteTop).isGreaterThanOrEqualTo(MIN_REMOTE)
            assertThat(layout.chevronTop - layout.openTop).isGreaterThanOrEqualTo(MIN_OPEN)
            assertThat(layout.chevronBottom).isLessThanOrEqualTo(height - 24.dp - MIN_ROOM)
            assertThat(layout.archHeight).isGreaterThan(layout.archWidth / 2)
        }
    }

    @Test
    fun `avec un texte agrandi, le titre garde sa place au-dessus de la télécommande`() {
        val layout = listenLayout(width = 411.dp, height = 923.dp, fontScale = 1.5f)

        assertThat(layout.remoteTop - layout.titlesTop).isGreaterThanOrEqualTo(MIN_TITLES * 1.5f)
    }

    private companion object {
        const val TOLERANCE = 0.5f
        val MIN_ROOM = 16.dp
        val MIN_TIMES = 28.dp
        val MIN_TITLES = 93.dp
        val MIN_REMOTE = 84.dp
        val MIN_OPEN = 44.dp
        val SIZES = listOf(393.dp to 780.dp, 360.dp to 640.dp, 320.dp to 640.dp, 360.dp to 808.dp, 320.dp to 560.dp)
    }
}
