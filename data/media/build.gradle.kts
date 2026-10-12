plugins {
    id("bercail.android.data")
}

android {
    namespace = "io.github.bahuauddimitri.bercail.data.media"
}

dependencies {
    implementation(project(":core:domain"))
    // The two colors of a cover, which tint Brume.
    implementation(libs.androidx.palette)

    testImplementation(project(":core:testing"))
    testImplementation(libs.kotlinx.coroutines.test)
}
