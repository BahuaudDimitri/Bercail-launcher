plugins {
    id("bercail.android.application")
    id("bercail.screenshots")
}

android {
    namespace = "io.github.bahuauddimitri.bercail"

    defaultConfig {
        applicationId = "io.github.bahuauddimitri.bercail"
        // Set by the release job from the Conventional Commits; local builds stay "dev".
        versionCode = providers.gradleProperty("bercail.versionCode").orNull?.toInt() ?: 1
        versionName = providers.gradleProperty("bercail.versionName").orNull ?: "0.0.0-dev"
    }

    signingConfigs {
        // The release key only exists in GitHub secrets and 1Password; without it, the release APK stays unsigned.
        val keystore = providers.environmentVariable("BERCAIL_KEYSTORE_FILE").orNull
        if (keystore != null) {
            create("release") {
                storeFile = file(keystore)
                storePassword = providers.environmentVariable("BERCAIL_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("BERCAIL_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("BERCAIL_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("release")
        }
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:domain"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)

    testImplementation(project(":core:testing"))
    testImplementation(libs.junit4)
    testImplementation(libs.assertk)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.test.espresso.core)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.assertk)
}

// Every local build points Git to the versioned hooks (formatting before commit, full check before push).
tasks.named("preBuild") { dependsOn(rootProject.tasks.named("installGitHooks")) }
