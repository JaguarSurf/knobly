plugins {
    alias(libs.plugins.knobly.kmp.compose)
    alias(libs.plugins.roborazzi)
}

kotlin {
    sourceSets {
        jvmTest.dependencies {
            implementation(libs.compose.ui.test)
            implementation(compose.desktop.currentOs)
            implementation(libs.roborazzi.compose.desktop)
        }
    }
}
