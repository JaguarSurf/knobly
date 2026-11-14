package app.knobly.buildlogic

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/** The `libs` version catalog (gradle/libs.versions.toml). */
val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun VersionCatalog.version(alias: String): String = findVersion(alias).get().requiredVersion

fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> = findLibrary(alias).get()

/** Bytecode level for Android and JVM targets. Android supports Java 17 on every API level we target. */
internal val KNOBLY_JAVA_VERSION = JavaVersion.VERSION_17
internal val KNOBLY_JVM_TARGET = JvmTarget.JVM_17

/**
 * Android namespace derived from the Gradle path, so it never has to be repeated:
 * `:core:model` -> `app.knobly.core.model`, `:platform:android-screening` -> `app.knobly.platform.androidscreening`.
 */
val Project.knoblyNamespace: String
    get() = "app.knobly" + path.split(":").filter { it.isNotEmpty() }.joinToString(separator = "") { ".${it.replace("-", "")}" }
