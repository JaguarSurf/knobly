import app.knobly.buildlogic.KNOBLY_JVM_TARGET
import app.knobly.buildlogic.knoblyNamespace
import app.knobly.buildlogic.libs
import app.knobly.buildlogic.version
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Shared Kotlin Multiplatform module: Android + iOS + JVM (desktop harness and headless tests).
 * See docs/adr/0005-desktop-harness-and-platform-ports.md.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("com.android.kotlin.multiplatform.library")
            pluginManager.apply("knobly.quality")

            extensions.configure<KotlinMultiplatformExtension> {
                (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android") {
                    namespace = knoblyNamespace
                    compileSdk = libs.version("android-compileSdk").toInt()
                    minSdk = libs.version("android-minSdk").toInt()
                    buildToolsVersion = libs.version("android-buildTools")
                    compilerOptions { jvmTarget.set(KNOBLY_JVM_TARGET) }
                    // Run commonTest on the Android host JVM too (no device needed). Android resources are
                    // included so Robolectric/Roborazzi-based host tests can be added without extra setup.
                    withHostTest { isIncludeAndroidResources = true }
                }

                jvm {
                    compilerOptions { jvmTarget.set(KNOBLY_JVM_TARGET) }
                }

                iosArm64()
                iosSimulatorArm64()

                sourceSets.getByName("commonTest").dependencies {
                    implementation(kotlin("test"))
                }
            }
        }
    }
}
