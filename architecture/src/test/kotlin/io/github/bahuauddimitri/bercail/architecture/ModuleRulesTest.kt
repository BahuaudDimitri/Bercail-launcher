package io.github.bahuauddimitri.bercail.architecture

import assertk.assertThat
import assertk.assertions.isEmpty
import org.junit.Test

class ModuleRulesTest {
    @Test
    fun `le domaine ne dépend pas d'Android`() {
        val offenders = Repository.productionFilesOf(":core:domain").filter { file ->
            file.imports.any { it.name.startsWith("android.") || it.name.startsWith("androidx.") }
        }

        assertThat(offenders.map { it.relativePath }).isEmpty()
    }

    @Test
    fun `chaque module a son README`() {
        val missing = Repository.modules.filterNot { module ->
            Repository.buildFileOf(module).resolveSibling("README.md").exists()
        }

        assertThat(missing).isEmpty()
    }
}
