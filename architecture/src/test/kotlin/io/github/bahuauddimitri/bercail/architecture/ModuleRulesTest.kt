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
    fun `les noms des tests instrumentés n'utilisent que des lettres, des chiffres, des espaces, le tiret et ’`() {
        // Android's DEX format rejects most punctuation in method names: the straight apostrophe, the comma,
        // the colon… It accepts letters, digits, spaces, the hyphen and the typographic apostrophe ’.
        val name = Regex("""fun `([^`]*)`""")
        val allowed = Regex("""[\p{L}\p{N} ’-]*""")
        val offenders = Repository.root.walkTopDown()
            .onEnter { it.name != "build" && it.name != ".gradle" }
            .filter { it.extension == "kt" && "/src/androidTest/" in it.invariantSeparatorsPath }
            .flatMap { file -> name.findAll(file.readText()).map { it.groupValues[1] } }
            .filterNot { allowed.matches(it) }
            .toList()

        assertThat(offenders).isEmpty()
    }

    @Test
    fun `chaque module a son README`() {
        val missing = Repository.modules.filterNot { module ->
            Repository.buildFileOf(module).resolveSibling("README.md").exists()
        }

        assertThat(missing).isEmpty()
    }
}
