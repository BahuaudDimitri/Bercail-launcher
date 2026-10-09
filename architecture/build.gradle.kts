plugins {
    id("bercail.jvm.library")
}

dependencies {
    testImplementation(libs.konsist)
    testImplementation(libs.junit4)
    testImplementation(libs.assertk)
}

tasks.test {
    // These tests read the whole repository: declare it as input, or Gradle would skip them after a change elsewhere.
    inputs
        .files(
            fileTree(rootDir) {
                include("**/src/**/*.kt", "**/src/**/AndroidManifest.xml", "**/build.gradle.kts")
                include("settings.gradle.kts", "docs/CATALOG.md")
                exclude("**/build/**", ".gradle/**", "build-logic/**")
            }
        ).withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("repositorySources")
    systemProperty("bercail.rootDir", rootDir.absolutePath)
}
