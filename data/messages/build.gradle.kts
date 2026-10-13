plugins {
    id("bercail.android.data")
}

android {
    namespace = "io.github.bahuauddimitri.bercail.data.messages"
}

dependencies {
    implementation(project(":core:domain"))
    // The file format of what was already seen; where the file lives is decided by the app.
    implementation(libs.androidx.datastore.preferences.core)

    testImplementation(project(":core:testing"))
    testImplementation(libs.kotlinx.coroutines.test)
}
