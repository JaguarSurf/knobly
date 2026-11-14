plugins {
    alias(libs.plugins.knobly.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.domain.rules)
            api(projects.core.database)
            api(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}
