import com.diffplug.gradle.spotless.SpotlessExtension
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register

/** Root project: coverage over all modules and the versioned Git hooks. */
class RootConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("org.jetbrains.kotlinx.kover")
        // Intermediate folders such as :core have no build file and nothing to measure.
        subprojects.filter { it.buildFile.exists() }.forEach { dependencies.add("kover", it) }
        extensions.configure<KoverProjectExtension> {
            reports {
                filters.excludes {
                    classes("*ComposableSingletons*", "*.BuildConfig", "*_Factory*")
                }
                verify.rule("Couverture globale") { minBound(GLOBAL_MIN_COVERAGE) }
            }
        }

        // The root build files and the build logic follow the same formatting as the modules.
        pluginManager.apply("com.diffplug.spotless")
        val ktlintVersion = libs.findVersion("ktlint").get().requiredVersion
        extensions.configure<SpotlessExtension> {
            kotlin {
                target("build-logic/convention/src/**/*.kt")
                ktlint(ktlintVersion)
            }
            kotlinGradle {
                target("*.gradle.kts", "build-logic/**/*.gradle.kts")
                ktlint(ktlintVersion)
            }
        }

        tasks.register<Exec>("installGitHooks") {
            description = "Points Git to the versioned hooks in config/git-hooks."
            commandLine("git", "config", "core.hooksPath", "config/git-hooks")
        }
    }

    private companion object {
        const val GLOBAL_MIN_COVERAGE = 80
    }
}
