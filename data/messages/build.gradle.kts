plugins {
    id("bercail.android.data")
}

android {
    namespace = "io.github.bahuauddimitri.bercail.data.messages"
}

dependencies {
    implementation(project(":core:domain"))

    testImplementation(project(":core:testing"))
}
