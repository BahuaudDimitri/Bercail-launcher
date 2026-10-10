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
    // Every source is a flow: nothing runs while nobody listens.
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit4)
    testImplementation(libs.assertk)
}
