import app.knobly.buildlogic.libs
import app.knobly.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** Adds Compose Multiplatform (shared UI) to a `knobly.kmp.library` module. */
class KmpComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("knobly.kmp.library")
            pluginManager.apply("org.jetbrains.compose")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            // Every module with composables must apply Compose Hot Reload for its changes to be reloaded
            // by the desktop harness (`./gradlew :app:desktop:hotRun`).
            pluginManager.apply("org.jetbrains.compose.hot-reload")

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.getByName("commonMain").dependencies {
                    implementation(libs.library("compose-runtime"))
                    implementation(libs.library("compose-foundation"))
                    implementation(libs.library("compose-ui"))
                    implementation(libs.library("compose-material3"))
                    implementation(libs.library("compose-ui-tooling-preview"))
                }
            }
        }
    }
}
