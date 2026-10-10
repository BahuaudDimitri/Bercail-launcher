import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Android library without Compose: the data modules, which talk to the phone and draw nothing. */
class AndroidDataConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("com.android.library")
        extensions.configure<LibraryExtension> { configureAndroid(this) }
        // The same tests as everywhere else, minus the Compose ones: JUnit + AssertK on Robolectric.
        listOf("junit4", "assertk", "robolectric", "androidx-test-core", "androidx-test-ext-junit").forEach {
            dependencies.add("testImplementation", libs.findLibrary(it).get())
        }
        configureQuality()
    }
}
