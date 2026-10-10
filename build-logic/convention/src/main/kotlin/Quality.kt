import com.diffplug.gradle.spotless.SpotlessExtension
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

/** Modules allowed to use Compose Material and raw styling: everything else goes through the design system. */
internal val DESIGN_SYSTEM_OWNERS = setOf(":app", ":core:designsystem")

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/** Formatting (ktlint via Spotless), static analysis (detekt), coverage (Kover) and test suites, for every module. */
internal fun Project.configureQuality() {
    pluginManager.apply("com.diffplug.spotless")
    pluginManager.apply("dev.detekt")
    pluginManager.apply("org.jetbrains.kotlinx.kover")

    val ktlintVersion = libs.findVersion("ktlint").get().requiredVersion
    extensions.configure<SpotlessExtension> {
        kotlin {
            target("src/**/*.kt")
            ktlint(ktlintVersion)
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktlint(ktlintVersion)
        }
    }

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig.set(true)
        parallel.set(true)
        source.setFrom(
            "src/main/kotlin",
            "src/test/kotlin",
            "src/androidTest/kotlin",
            "src/debug/kotlin",
            "src/release/kotlin"
        )
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        if (path !in DESIGN_SYSTEM_OWNERS) {
            config.from(rootProject.file("config/detekt/screens.yml"))
        }
    }
    tasks.named("check") { dependsOn("detekt") }

    configureTestSuites()
}

/**
 * Test suites, run separately with -Pbercail.suite=<name>:
 * - unit: plain JVM tests,
 * - ui: Robolectric and Compose tests (classes named *UiTest),
 * - screenshots: Roborazzi comparisons (classes named *ScreenshotTest).
 * Without the property, every test runs.
 */
private fun Project.configureTestSuites() {
    val suite = providers.gradleProperty("bercail.suite").orNull
    tasks.withType<Test>().configureEach {
        // Robolectric reaches into JDK internals to emulate Android 17.
        jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED", "--enable-native-access=ALL-UNNAMED")
        filter.isFailOnNoMatchingTests = false
        when (suite) {
            null -> Unit

            "unit" -> {
                filter.excludeTestsMatching("*UiTest")
                filter.excludeTestsMatching("*ScreenshotTest")
            }

            "ui" -> filter.includeTestsMatching("*UiTest")

            "screenshots" -> filter.includeTestsMatching("*ScreenshotTest")

            else -> error("Unknown test suite '$suite': use unit, ui or screenshots.")
        }
    }
}
