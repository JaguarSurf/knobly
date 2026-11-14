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

/**
 * versionCode = number of commits on HEAD, so every APK built after a new commit installs as an update over the
 * previous one (Android refuses downgrades). Builds without the git history (source archives, shallow clones) get
 * 1 with a warning, and release builds fail: pass `-Pknobly.versionCode=<n>` or use the full history.
 * See docs/adr/0007-app-updates-and-data-compatibility.md.
 */
val knoblyVersionCode: Int =
    providers.gradleProperty("knobly.versionCode").orNull?.toInt()
        ?: gitOutput("rev-parse", "--is-shallow-repository")
            .takeIf { it == "false" }
            ?.let { gitOutput("rev-list", "--count", "HEAD")?.toIntOrNull() }
        ?: run {
            val message =
                "Cannot compute versionCode from the git history (no git, or a shallow clone). " +
                    "Pass -Pknobly.versionCode=<n> or use the full history."
            check(gradle.startParameter.taskNames.none { it.contains("release", ignoreCase = true) }) { message }
            logger.warn("$message Using 1.")
            1
        }

fun gitOutput(vararg args: String): String? =
    runCatching {
        providers
            .exec {
                commandLine("git", *args)
                isIgnoreExitValue = true
            }.standardOutput.asText
            .get()
            .trim()
            .ifEmpty { null }
    }.getOrNull()

android {
    defaultConfig {
        applicationId = "app.knobly"
        versionCode = knoblyVersionCode
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
        debug {
            // Shown in the system's App info, to tell which build is installed: "0.1.0-debug (42)".
            versionNameSuffix = "-debug ($knoblyVersionCode)"
        }
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
