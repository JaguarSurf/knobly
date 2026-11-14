plugins {
    alias(libs.plugins.knobly.kmp.compose)
    alias(libs.plugins.roborazzi)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Bundled fonts (Poppins, Inter) live in src/commonMain/composeResources/font.
            implementation(libs.compose.components.resources)
        }
        jvmTest.dependencies {
            implementation(libs.compose.ui.test)
            implementation(compose.desktop.currentOs)
            implementation(libs.roborazzi.compose.desktop)
        }
    }
}

compose.resources {
    packageOfResClass = "app.knobly.core.designsystem.resources"
}
