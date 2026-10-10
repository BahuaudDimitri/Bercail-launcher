plugins {
    id("bercail.jvm.library")
}

dependencies {
    implementation(project(":core:domain"))
    // The file format only: where the file lives on the phone is decided by the app.
    api(libs.androidx.datastore.preferences.core)

    testImplementation(libs.junit4)
    testImplementation(libs.assertk)
    testImplementation(libs.kotlinx.coroutines.test)
}
