plugins {
    id("bercail.jvm.library")
}

dependencies {
    api(project(":core:domain"))

    testImplementation(libs.junit4)
    testImplementation(libs.assertk)
    testImplementation(libs.kotlinx.coroutines.test)
}
