import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("com.android.application")
        extensions.configure<ApplicationExtension> {
            configureAndroid(this)
            configureCompose(this)
            defaultConfig.targetSdk = TARGET_SDK
            lint.checkDependencies = true
        }
        configureQuality()
        // The instrumented tests only run on a device, but they must at least build before every push.
        tasks.named("check") { dependsOn(tasks.matching { it.name == "assembleDebugAndroidTest" }) }
    }
}
