plugins {
    alias(libs.plugins.knobly.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.domain.rules)
            api(projects.domain.history)
        }
    }
}
