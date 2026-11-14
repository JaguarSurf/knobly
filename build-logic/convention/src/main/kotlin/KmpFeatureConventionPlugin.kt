import app.knobly.buildlogic.libs
import app.knobly.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * A `:feature:*` module: shared Compose UI + ViewModel + Koin, on top of the design system and the model.
 * Features never depend on other features, and never call platform APIs directly (use domain ports).
 */
class KmpFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("knobly.kmp.compose")

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.getByName("commonMain").dependencies {
                    implementation(project(":core:designsystem"))
                    implementation(project(":core:model"))
                    implementation(libs.library("androidx-lifecycle-viewmodel-compose"))
                    implementation(libs.library("androidx-lifecycle-runtime-compose"))
                    implementation(libs.library("koin-compose"))
                    implementation(libs.library("koin-compose-viewmodel"))
                }
            }
        }
    }
}
