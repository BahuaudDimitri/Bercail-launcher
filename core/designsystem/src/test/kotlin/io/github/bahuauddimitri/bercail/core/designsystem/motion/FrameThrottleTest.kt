package io.github.bahuauddimitri.bercail.core.designsystem.motion

import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import org.junit.Test

class FrameThrottleTest {
    @Test
    fun `sur un écran à 120 Hz, une animation ne dessine que 20 images par seconde`() {
        val throttle = FrameThrottle(fps = 20)
        val oneSecondAt120Hz = (0 until 120).map { it * NANOS_PER_SECOND / 120 }

        val drawn = oneSecondAt120Hz.count { throttle.shouldDraw(it) }

        assertThat(drawn).isBetween(19, 20)
    }

    @Test
    fun `sur un écran à 60 Hz, une animation ne dessine que 20 images par seconde`() {
        val throttle = FrameThrottle(fps = 20)
        val oneSecondAt60Hz = (0 until 60).map { it * NANOS_PER_SECOND / 60 }

        val drawn = oneSecondAt60Hz.count { throttle.shouldDraw(it) }

        assertThat(drawn).isEqualTo(20)
    }

    @Test
    fun `la première image est toujours dessinée`() {
        assertThat(FrameThrottle(fps = 20).shouldDraw(123_456_789L)).isEqualTo(true)
    }

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000L
    }
}
