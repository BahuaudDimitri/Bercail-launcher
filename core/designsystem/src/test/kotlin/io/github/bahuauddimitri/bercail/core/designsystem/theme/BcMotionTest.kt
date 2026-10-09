package io.github.bahuauddimitri.bercail.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.Test

class BcMotionTest {
    @Test
    fun `les tiroirs s'animent en 400 ms avec la courbe du prototype`() {
        val motion = BcMotion.forAnimatorScale(1f)

        assertThat(motion.drawerMillis).isEqualTo(400)
        assertThat(motion.easing).isEqualTo(CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f))
        assertThat(motion.reduced).isFalse()
    }

    @Test
    fun `les animations s'arrêtent quand Android demande de les supprimer`() {
        val motion = BcMotion.forAnimatorScale(0f)

        assertThat(motion.reduced).isTrue()
        assertThat(motion.drawerMillis).isEqualTo(0)
        assertThat(motion.fadeMillis).isEqualTo(0)
        assertThat(motion.timelineMillis).isEqualTo(0)
    }

    @Test
    fun `les animations suivent la vitesse choisie dans Android`() {
        val motion = BcMotion.forAnimatorScale(0.5f)

        assertThat(motion.drawerMillis).isEqualTo(200)
        assertThat(motion.reduced).isFalse()
    }
}
