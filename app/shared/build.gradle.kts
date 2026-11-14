plugins {
    alias(libs.plugins.knobly.kmp.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.roborazzi)
}

kotlin {
    // Framework consumed by the Xcode project (iosApp): `import Shared`.
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.domain.rules)
            implementation(projects.core.designsystem)
            implementation(projects.feature.calls)
            implementation(projects.feature.history)
            implementation(projects.feature.stats)
            implementation(projects.feature.settings)
            implementation(projects.feature.onboarding)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.kotlinx.serialization.json)
            api(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
        }
        iosMain.dependencies {
            implementation(projects.platform.iosCalldirectory)
        }
        jvmTest.dependencies {
            implementation(projects.platform.fake)
            implementation(libs.compose.ui.test)
            implementation(compose.desktop.currentOs)
            implementation(libs.roborazzi.compose.desktop)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
