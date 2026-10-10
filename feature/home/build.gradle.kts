plugins {
    id("bercail.android.library")
    id("bercail.screenshots")
}

android {
    namespace = "io.github.bahuauddimitri.bercail.feature.home"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:domain"))
    // The screen only listens to its sources while it is visible.
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    // The window of the home screen: its wallpaper is hidden while Brume covers it.
    implementation(libs.androidx.activity.compose)

    testImplementation(project(":core:testing"))
    testImplementation(libs.kotlinx.coroutines.test)
}
