package io.github.bahuauddimitri.bercail.architecture

import assertk.assertThat
import assertk.assertions.isEmpty
import com.lemonappdev.konsist.api.ext.list.functions
import org.junit.Test

/** docs/CATALOG.md says where everything is: it must follow the code, in the same commit. */
class CatalogTest {
    @Test
    fun `chaque module figure au catalogue`() {
        val missing = Repository.modules.filterNot { "`$it`" in Repository.catalog }

        assertThat(missing).isEmpty()
    }

    @Test
    fun `chaque composant public du design system figure au catalogue`() {
        val components = Repository.productionFilesOf(":core:designsystem")
            .functions()
            .filter { it.hasAnnotationWithName("Composable") && it.hasPublicOrDefaultModifier }
            .filter { it.name.startsWith("Bc") }
            .map { it.name }

        val missing = components.filterNot { "`$it`" in Repository.catalog }

        assertThat(missing).isEmpty()
    }

    @Test
    fun `chaque composant public du design system figure dans la galerie`() {
        val gallery = Repository.productionFilesOf(":core:designsystem")
            .filter { it.relativePath.endsWith("/gallery/BcGallery.kt") }
            .joinToString("\n") { it.text }
        val components = Repository.productionFilesOf(":core:designsystem")
            .functions()
            .filter { it.hasAnnotationWithName("Composable") && it.hasPublicOrDefaultModifier }
            .map { it.name }
            .filter { it.startsWith("Bc") && it != "BcGallery" && it != "BcTheme" }

        val missing = components.filterNot { Regex("""\b$it\(""").containsMatchIn(gallery) }

        assertThat(missing).isEmpty()
    }

    @Test
    fun `chaque fichier cité au catalogue existe`() {
        val cited = Regex("""`([\w./-]+\.(kt|md|xml|kts))`""").findAll(Repository.catalog).map { it.groupValues[1] }

        val missing = cited.filterNot { Repository.root.resolve(it).exists() }.toList()

        assertThat(missing).isEmpty()
    }
}
