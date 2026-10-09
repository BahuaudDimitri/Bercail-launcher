plugins {
    id("bercail.android.library")
    id("bercail.screenshots")
}

android {
    namespace = "io.github.bahuauddimitri.bercail.core.designsystem"
}

dependencies {
    // Screens build their layouts with Compose UI and Foundation, and get every visual from this module.
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.foundation)
}
