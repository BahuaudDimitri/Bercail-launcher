import io.github.takahirom.roborazzi.RoborazziExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Roborazzi screenshot tests on Robolectric, with references committed next to the tests. */
class ScreenshotsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("io.github.takahirom.roborazzi")
        extensions.configure<RoborazziExtension> {
            outputDir.set(layout.projectDirectory.dir("src/test/screenshots"))
        }
        listOf("roborazzi", "roborazzi-compose", "roborazzi-junit-rule").forEach { alias ->
            dependencies.add("testImplementation", libs.findLibrary(alias).get())
        }
    }
}
