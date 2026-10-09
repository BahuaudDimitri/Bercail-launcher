package io.github.bahuauddimitri.bercail.core.designsystem.component

import assertk.assertThat
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotEmpty
import org.junit.Test

class BcIconTest {
    @Test
    fun `chaque icône du prototype a un dessin`() {
        assertThat(BcIcons.entries).isNotEmpty()
        BcIcons.entries.forEach { icon ->
            assertThat(icon.vector.root.size).isGreaterThan(0)
        }
    }
}
