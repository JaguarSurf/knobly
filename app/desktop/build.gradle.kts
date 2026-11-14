import org.jetbrains.compose.reload.gradle.ComposeHotRun

// Development harness only (docs/adr/0005-desktop-harness-and-platform-ports.md): runs the shared UI with fake
// platform adapters and an incoming call simulator. Never distributed.
//
// A Kotlin Multiplatform module with a single JVM target (not a plain Kotlin/JVM module): Compose Hot Reload only
// reloads project dependencies that are consumed this way.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.hot.reload)
    alias(libs.plugins.knobly.quality)
}

kotlin {
    jvmToolchain(21)
    jvm()

    sourceSets {
        jvmMain.dependencies {
            implementation(projects.app.shared)
            implementation(projects.platform.fake)
            implementation(projects.core.phone)
            implementation(projects.core.designsystem)
            implementation(compose.desktop.currentOs)
            implementation(libs.compose.material3)
            implementation(libs.koin.compose)
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}

val desktopMainClass = "app.knobly.app.desktop.MainKt"

compose.desktop {
    application {
        mainClass = desktopMainClass
    }
}

// `./gradlew :app:desktop:hotRun` without having to pass --mainClass.
tasks.withType<ComposeHotRun>().configureEach {
    mainClass.set(desktopMainClass)
}

// Skia render API for the harness window. SOFTWARE works everywhere (no GPU/OpenGL needed); OPENGL uses the GPU.
// Precedence: -Pknobly.skikoRenderApi=..., then the KNOBLY_SKIKO_RENDER_API environment variable (set by the
// devcontainer when an NVIDIA GPU is available), then SOFTWARE.
val skikoRenderApi =
    providers
        .gradleProperty("knobly.skikoRenderApi")
        .orElse(providers.environmentVariable("KNOBLY_SKIKO_RENDER_API"))
        .orElse("SOFTWARE")
tasks.withType<JavaExec>().configureEach {
    systemProperty("skiko.renderApi", skikoRenderApi.get())
}
