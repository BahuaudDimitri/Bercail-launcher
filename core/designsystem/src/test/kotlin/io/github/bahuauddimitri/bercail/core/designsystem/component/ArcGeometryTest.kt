package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.ui.geometry.Offset
import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.math.sqrt
import org.junit.Test

/**
 * How a finger on the half circle of the Listen screen becomes a position in the track: by its angle around the
 * centre of the arch, from the left end (the start) through the bottom (the middle) to the right end (the end).
 */
class ArcGeometryTest {
    @Test
    fun `le bout gauche de l'arc est le début, le bas le milieu, le bout droit la fin`() {
        assertThat(arcFraction(Offset(-R, 0f))).isCloseTo(0f, EPSILON)
        assertThat(arcFraction(Offset(0f, R))).isCloseTo(0.5f, EPSILON)
        assertThat(arcFraction(Offset(R, 0f))).isCloseTo(1f, EPSILON)
    }

    @Test
    fun `seul l'angle compte, pas la distance au centre`() {
        val diagonal = R / sqrt(2f)
        assertThat(arcFraction(Offset(-diagonal, diagonal))).isCloseTo(0.25f, EPSILON)
        assertThat(arcFraction(Offset(-diagonal * 1.1f, diagonal * 1.1f))).isCloseTo(0.25f, EPSILON)
        assertThat(arcFraction(Offset(diagonal, diagonal))).isCloseTo(0.75f, EPSILON)
    }

    @Test
    fun `au-dessus des bouts, la position s'arrête au début ou à la fin du demi-cercle`() {
        assertThat(arcFraction(Offset(-R, -10f))).isCloseTo(0f, EPSILON)
        assertThat(arcFraction(Offset(-1f, -R))).isCloseTo(0f, EPSILON)
        assertThat(arcFraction(Offset(R, -10f))).isCloseTo(1f, EPSILON)
        assertThat(arcFraction(Offset(1f, -R))).isCloseTo(1f, EPSILON)
    }

    @Test
    fun `chaque position a son point sur l'arc, et ce point redonne la position`() {
        listOf(0f, 0.3f, 0.5f, 0.82f, 1f).forEach { fraction ->
            val point = arcPoint(fraction, R)
            assertThat(point.getDistance()).isCloseTo(R, EPSILON)
            assertThat(arcFraction(point)).isCloseTo(fraction, EPSILON)
        }
    }

    @Test
    fun `la bande qui se touche longe l'arc sur 40 dp, pas le centre ni le dessus`() {
        assertThat(inArcBand(Offset(0f, R), R, HALF_BAND)).isTrue()
        assertThat(inArcBand(Offset(0f, R + HALF_BAND - 1f), R, HALF_BAND)).isTrue()
        assertThat(inArcBand(Offset(0f, R - HALF_BAND + 1f), R, HALF_BAND)).isTrue()
        assertThat(inArcBand(Offset(-R, -HALF_BAND + 1f), R, HALF_BAND)).isTrue()
        assertThat(inArcBand(Offset(0f, R + HALF_BAND + 1f), R, HALF_BAND)).isFalse()
        assertThat(inArcBand(Offset(0f, R / 2), R, HALF_BAND)).isFalse()
        assertThat(inArcBand(Offset(0f, 0f), R, HALF_BAND)).isFalse()
        assertThat(inArcBand(Offset(-R, -HALF_BAND - 1f), R, HALF_BAND)).isFalse()
        assertThat(inArcBand(Offset(0f, -R), R, HALF_BAND)).isFalse()
    }

    private companion object {
        const val R = 164f
        const val HALF_BAND = 20f
        const val EPSILON = 0.001f
    }
}
