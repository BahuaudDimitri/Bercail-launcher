plugins {
    id("bercail.android.data")
}

android {
    namespace = "io.github.bahuauddimitri.bercail.data.apps"
}

dependencies {
    implementation(project(":core:domain"))
}
