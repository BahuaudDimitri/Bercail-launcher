package io.github.bahuauddimitri.bercail.architecture

import assertk.assertThat
import assertk.assertions.isEmpty
import org.junit.Test

/** "Design system only": screens are built from core:designsystem components, never from raw Compose styling. */
class DesignSystemOnlyTest {
    private val screenFiles = Repository.screenModules.flatMap(Repository::productionFilesOf)

    @Test
    fun `les écrans n'importent pas Compose Material`() {
        val offenders = screenFiles.filter { file ->
            file.imports.any { it.name.startsWith("androidx.compose.material") }
        }

        assertThat(offenders.map { it.relativePath }).isEmpty()
    }

    @Test
    fun `les écrans n'utilisent ni BasicText ni BasicTextField`() {
        val offenders = screenFiles.filter { file -> Regex("""\bBasicText(Field)?\b""").containsMatchIn(file.text) }

        assertThat(offenders.map { it.relativePath }).isEmpty()
    }

    @Test
    fun `les écrans n'écrivent pas de couleur en dur`() {
        val offenders = screenFiles.filter { file -> Regex("""\bColor\(\s*0x""").containsMatchIn(file.text) }

        assertThat(offenders.map { it.relativePath }).isEmpty()
    }

    @Test
    fun `les écrans n'écrivent pas de taille de texte en dur`() {
        val offenders = screenFiles.filter { file -> Regex("""\b\d+(\.\d+)?f?\.sp\b""").containsMatchIn(file.text) }

        assertThat(offenders.map { it.relativePath }).isEmpty()
    }

    @Test
    fun `seuls l'app et le design system dépendent de Compose Material`() {
        val offenders = Repository.screenModules.filter { module ->
            Repository.buildFileOf(module).readText().contains("material", ignoreCase = true)
        }

        assertThat(offenders).isEmpty()
    }
}
