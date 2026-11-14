plugins {
    `kotlin-dsl`
}

group = "app.knobly.buildlogic"

kotlin {
    jvmToolchain(21)
}

dependencies {
    // compileOnly: the plugins themselves are put on the classpath by the root build.gradle.kts,
    // so all modules share a single copy of each plugin.
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.compose.gradle.plugin)
    compileOnly(libs.compose.compiler.gradle.plugin)
    compileOnly(libs.detekt.gradle.plugin)
    compileOnly(libs.ktlint.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = libs.plugins.knobly.kmp.library.get().pluginId
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("kmpCompose") {
            id = libs.plugins.knobly.kmp.compose.get().pluginId
            implementationClass = "KmpComposeConventionPlugin"
        }
        register("kmpFeature") {
            id = libs.plugins.knobly.kmp.feature.get().pluginId
            implementationClass = "KmpFeatureConventionPlugin"
        }
        register("androidApplication") {
            id = libs.plugins.knobly.android.application.get().pluginId
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = libs.plugins.knobly.android.library.get().pluginId
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("quality") {
            id = libs.plugins.knobly.quality.get().pluginId
            implementationClass = "QualityConventionPlugin"
        }
    }
}
