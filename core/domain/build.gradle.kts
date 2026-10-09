plugins {
    id("bercail.jvm.library")
}

kover {
    reports {
        verify {
            rule("Couverture du domaine") { minBound(90) }
        }
    }
}

dependencies {
    testImplementation(libs.junit4)
    testImplementation(libs.assertk)
}
