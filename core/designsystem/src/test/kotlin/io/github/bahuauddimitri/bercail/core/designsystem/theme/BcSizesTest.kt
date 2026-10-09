package io.github.bahuauddimitri.bercail.core.designsystem.theme

import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isNotEmpty
import org.junit.Test

class BcSizesTest {
    @Test
    fun `chaque cible tactile fait au moins 40 dp`() {
        assertThat(BcSizes.touchTargets).isNotEmpty()
        BcSizes.touchTargets.values.forEach { size ->
            assertThat(size).isGreaterThanOrEqualTo(40.dp)
        }
    }
}
