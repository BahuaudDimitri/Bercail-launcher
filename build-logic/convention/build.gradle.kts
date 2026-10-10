plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.compose.gradlePlugin)
    implementation(libs.detekt.gradlePlugin)
    implementation(libs.spotless.gradlePlugin)
    implementation(libs.kover.gradlePlugin)
    implementation(libs.roborazzi.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("root") {
            id = "bercail.root"
            implementationClass = "RootConventionPlugin"
        }
        register("androidApplication") {
            id = "bercail.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "bercail.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidData") {
            id = "bercail.android.data"
            implementationClass = "AndroidDataConventionPlugin"
        }
        register("jvmLibrary") {
            id = "bercail.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("screenshots") {
            id = "bercail.screenshots"
            implementationClass = "ScreenshotsConventionPlugin"
        }
    }
}
