plugins {
    alias(libs.plugins.knobly.kmp.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    // Room's `expect object` database constructor.
    compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }

    sourceSets {
        commonMain.dependencies {
            api(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

// Exported schemas are versioned: they are what migrations are written and tested against.
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // Room generates the database implementation for every target.
    listOf(
        "kspAndroid",
        "kspJvm",
        "kspIosArm64",
        "kspIosSimulatorArm64",
    ).forEach { add(it, libs.androidx.room.compiler) }
}
