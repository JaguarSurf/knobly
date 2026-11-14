import app.knobly.buildlogic.KNOBLY_JAVA_VERSION
import app.knobly.buildlogic.knoblyNamespace
import app.knobly.buildlogic.libs
import app.knobly.buildlogic.version
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Android-only library (platform adapters such as `:platform:android-screening`). Uses AGP 9 built-in Kotlin. */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            pluginManager.apply("knobly.quality")

            extensions.configure<LibraryExtension> {
                namespace = knoblyNamespace
                compileSdk = libs.version("android-compileSdk").toInt()
                buildToolsVersion = libs.version("android-buildTools")
                defaultConfig {
                    minSdk = libs.version("android-minSdk").toInt()
                }
                compileOptions {
                    sourceCompatibility = KNOBLY_JAVA_VERSION
                    targetCompatibility = KNOBLY_JAVA_VERSION
                }
            }
        }
    }
}
