plugins {
    id("bercail.android.data")
}

android {
    namespace = "io.github.bahuauddimitri.bercail.data.update"
}

dependencies {
    implementation(project(":core:domain"))
    // The file format only: where the file lives on the phone is decided by the app.
    implementation(libs.androidx.datastore.preferences.core)

    testImplementation(libs.mockwebserver)
}
