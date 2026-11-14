plugins {
    alias(libs.plugins.knobly.kmp.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.domain.stats)
        }
        commonTest.dependencies {
            implementation(projects.platform.fake)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
