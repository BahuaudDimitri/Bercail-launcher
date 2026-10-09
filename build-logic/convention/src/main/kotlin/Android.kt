import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.HasHostTestsBuilder
import com.android.build.api.variant.HostTestBuilder
import org.gradle.api.JavaVersion
import org.gradle.api.Project

internal const val COMPILE_SDK = 37
internal const val MIN_SDK = 36
internal const val TARGET_SDK = 37

internal fun Project.configureAndroid(android: CommonExtension) {
    android.apply {
        compileSdk = COMPILE_SDK
        defaultConfig.minSdk = MIN_SDK
        defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        compileOptions.sourceCompatibility = JavaVersion.VERSION_17
        compileOptions.targetCompatibility = JavaVersion.VERSION_17
        testOptions.unitTests.isIncludeAndroidResources = true
        lint.apply {
            warningsAsErrors = true
            abortOnError = true
            // Dependency updates are Renovate's job, not a reason to fail the build.
            disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
        }
    }
    // Unit tests run on the debug variant only: the release variant adds time, not confidence.
    (extensions.getByName("androidComponents") as AndroidComponentsExtension<*, *, *>).apply {
        beforeVariants(selector().withBuildType("release")) { variant ->
            (variant as? HasHostTestsBuilder)?.hostTests?.get(HostTestBuilder.UNIT_TEST_TYPE)?.enable = false
        }
    }
}

internal fun Project.configureCompose(android: CommonExtension) {
    pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
    android.buildFeatures.compose = true
    val bom = dependencies.platform(libs.findLibrary("androidx-compose-bom").get())
    dependencies.add("implementation", bom)
    dependencies.add("testImplementation", bom)
    dependencies.add("androidTestImplementation", bom)
}
