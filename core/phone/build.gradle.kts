import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    alias(libs.plugins.knobly.kmp.library)
}

kotlin {
    // `jvmAndroidMain` is shared by Android and the JVM: both use libphonenumber (a Java library).
    // iOS will get its own implementation (PhoneNumberKit, provided from Swift).
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("jvmAndroid") {
                withJvm()
                withCompilations { it.target.platformType == KotlinPlatformType.androidJvm }
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
        }
        getByName("jvmAndroidMain").dependencies {
            implementation(libs.libphonenumber)
        }
    }
}
