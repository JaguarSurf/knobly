import app.knobly.buildlogic.libs
import app.knobly.buildlogic.version
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jlleitschuh.gradle.ktlint.KtlintExtension

/** Static analysis for every module: Detekt (code smells) + ktlint (formatting). Both run as part of `check`. */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("io.gitlab.arturbosch.detekt")
            pluginManager.apply("org.jlleitschuh.gradle.ktlint")

            extensions.configure<DetektExtension> {
                buildUponDefaultConfig = true
                config.setFrom(rootProject.file("config/detekt/detekt.yml"))
                // Plain source scan of every source set (commonMain, androidMain, jvmTest, ...).
                source.setFrom(files("src"))
            }

            // Detekt 1.23.x embeds its own Kotlin compiler (2.0.x). Keep the project's newer Kotlin
            // version from being forced onto Detekt's classpath, which would make it fail at startup.
            configurations.matching { it.name == "detekt" }.configureEach {
                resolutionStrategy.eachDependency {
                    if (requested.group == "org.jetbrains.kotlin") {
                        useVersion(io.gitlab.arturbosch.detekt.getSupportedKotlinVersion())
                    }
                }
            }

            extensions.configure<KtlintExtension> {
                version.set(libs.version("ktlint"))
                filter {
                    exclude { it.file.path.contains("/build/") }
                }
            }
        }
    }
}
