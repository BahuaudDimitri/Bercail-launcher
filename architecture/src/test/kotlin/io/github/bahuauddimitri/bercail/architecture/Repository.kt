package io.github.bahuauddimitri.bercail.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import java.io.File

/** The repository seen by the architecture tests: modules, Kotlin sources, manifests and build files. */
internal object Repository {
    val root: File = File(checkNotNull(System.getProperty("bercail.rootDir")) { "Run these tests with Gradle." })

    /** Modules allowed to use Compose Material and raw styling. Every other module is a "screen" module. */
    private val designSystemOwners = setOf(":app", ":core:designsystem")

    /** Modules that hold tooling, not app code. */
    private val toolingModules = setOf(":architecture")

    val modules: List<String> by lazy {
        Regex("""include\("(:[^"]+)"\)""")
            .findAll(root.resolve("settings.gradle.kts").readText())
            .map { it.groupValues[1] }
            .toList()
    }

    val screenModules: List<String> get() = modules - designSystemOwners - toolingModules

    /** Production Kotlin files of the app, without the build logic. */
    val productionFiles: List<KoFileDeclaration> by lazy {
        Konsist.scopeFromProduction().files.filterNot { it.relativePath.startsWith("build-logic/") }
    }

    fun productionFilesOf(module: String): List<KoFileDeclaration> =
        productionFiles.filter { it.relativePath.startsWith(module.toDirectory() + "/") }

    val mainManifests: List<File> by lazy {
        modules.map { root.resolve(it.toDirectory()).resolve("src/main/AndroidManifest.xml") }.filter { it.exists() }
    }

    fun buildFileOf(module: String): File = root.resolve(module.toDirectory()).resolve("build.gradle.kts")

    val catalog: String by lazy { root.resolve("docs/CATALOG.md").readText() }

    private fun String.toDirectory() = removePrefix(":").replace(':', '/')
}

/** Path from the repository root, with forward slashes on every system. */
internal val KoFileDeclaration.relativePath: String
    get() = File(path).relativeTo(Repository.root).invariantSeparatorsPath
