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
    fun `les noms des tests instrumentés utilisent l'apostrophe typographique`() {
        // Android's DEX format rejects the straight apostrophe in method names, but accepts ’.
        val straightApostrophe = Regex("""fun `[^`]*'[^`]*`""")
        val offenders = Repository.root.walkTopDown()
            .onEnter { it.name != "build" && it.name != ".gradle" }
            .filter { it.extension == "kt" && "/src/androidTest/" in it.invariantSeparatorsPath }
            .filter { straightApostrophe.containsMatchIn(it.readText()) }
            .map { it.relativeTo(Repository.root).invariantSeparatorsPath }
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
