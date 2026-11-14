import app.knobly.buildlogic.KNOBLY_JAVA_VERSION
import app.knobly.buildlogic.knoblyNamespace
import app.knobly.buildlogic.libs
import app.knobly.buildlogic.version
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** The Android app module. AGP 9 compiles Kotlin itself (built-in Kotlin), so no kotlin-android plugin. */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            pluginManager.apply("knobly.quality")

            extensions.configure<ApplicationExtension> {
                namespace = knoblyNamespace
                compileSdk = libs.version("android-compileSdk").toInt()
                buildToolsVersion = libs.version("android-buildTools")
                defaultConfig {
                    minSdk = libs.version("android-minSdk").toInt()
                    targetSdk = libs.version("android-targetSdk").toInt()
                }
                compileOptions {
                    sourceCompatibility = KNOBLY_JAVA_VERSION
                    targetCompatibility = KNOBLY_JAVA_VERSION
                }
                buildFeatures {
                    compose = true
                }
            }
        }
    }
}
