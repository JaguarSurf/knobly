import java.util.Properties

plugins {
    alias(libs.plugins.knobly.android.application)
}

/**
 * Release signing, read from `keystore.properties` (repository root) or from environment variables.
 * Both are git-ignored / never committed. Without them, `assembleRelease` produces an unsigned APK.
 */
val keystoreProperties =
    Properties().apply {
        val file = rootProject.file("keystore.properties")
        if (file.exists()) file.inputStream().use(::load)
    }

fun signingValue(
    property: String,
    environmentVariable: String,
): String? = keystoreProperties.getProperty(property) ?: providers.environmentVariable(environmentVariable).orNull

val releaseStoreFile = signingValue("storeFile", "KNOBLY_KEYSTORE_FILE")

android {
    defaultConfig {
        applicationId = "app.knobly"
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = signingValue("storePassword", "KNOBLY_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "KNOBLY_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "KNOBLY_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }
}

dependencies {
    implementation(projects.app.shared)
    implementation(projects.platform.androidScreening)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.compose.runtime)
    implementation(libs.koin.android)
}

// APK/AAB file names: knobly-debug.apk, knobly-release.apk, ...
base {
    archivesName.set("knobly")
}
