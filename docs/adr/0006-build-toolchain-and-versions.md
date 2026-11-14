# ADR-0006: Build toolchain, versions, Gradle structure and release signing

- **Status:** accepted
- **Date:** 2026-11-13

## Context

The project needs a reproducible build that produces an installable Android APK from the devcontainer, compiles the iOS framework on macOS, and is easy to keep consistent across ~20 modules. Kotlin, Compose Multiplatform, the Android Gradle Plugin (AGP) and Gradle have strict compatibility ranges, so "latest of everything" is not always a valid combination.

## Decision

### Versions

Latest stable releases that are **inside each other's supported ranges** (checked on 2026-11-13):

| Component | Version | Why |
|---|---|---|
| Kotlin | 2.4.21 | Latest stable |
| Compose Multiplatform | 1.12.2 | Latest stable; based on Jetpack Compose 1.12.1 (requires compileSdk 37 and AGP 9) |
| AGP | 9.3.3 | Kotlin 2.4.20–2.4.21 supports AGP up to 9.3.x; 9.3.2+ fixes a lint crash on JDK 17 |
| Gradle | 9.7.1 | Kotlin 2.4.21 supports Gradle up to 9.7; AGP 9.3 requires at least 9.5 |
| JDK | 21 (devcontainer and CI) | LTS; bytecode target is Java 17 |
| Android SDK | compileSdk/targetSdk 37, minSdk 29, build-tools 37.0.0 | minSdk 29 because of `ROLE_CALL_SCREENING` |

AGP 9.5.1 + Gradle 9.9.1 are newer, but outside the range tested by Kotlin 2.4.21. Upgrade them together with the next Kotlin release that supports them.

KSP 2.3.13 runs the Room compiler (`:core:database`). KSP2 releases no longer follow the Kotlin version, so any recent KSP2 works with Kotlin 2.4.x; pick a release that has been out for a couple of weeks.

Detekt stays on 1.23.8 (latest stable; 2.0 is still alpha). It embeds its own Kotlin compiler, so the build pins Detekt's classpath to it.

### Gradle structure

- **Version catalog** (`gradle/libs.versions.toml`): the single place for versions and coordinates.
- **Convention plugins** (`build-logic/`, an included build): `knobly.kmp.library`, `knobly.kmp.compose`, `knobly.kmp.feature`, `knobly.android.application`, `knobly.android.library`, `knobly.quality`. Module build files only declare what is specific to the module.
- **KMP Android target** through the new `com.android.kotlin.multiplatform.library` plugin (AGP 9), and AGP's built-in Kotlin for Android-only modules (no `kotlin-android` plugin).
- Android namespaces are derived from the Gradle path (`:core:model` → `app.knobly.core.model`).
- Configuration cache and build cache are enabled.

### Release signing

- The release keystore and its passwords are **never committed**. They are read from `keystore.properties` at the repository root, or from the `KNOBLY_KEYSTORE_FILE`, `KNOBLY_KEYSTORE_PASSWORD`, `KNOBLY_KEY_ALIAS`, `KNOBLY_KEY_PASSWORD` environment variables (for CI secrets). Both `keystore.properties` and `*.jks`/`*.keystore` are git-ignored.
- Without credentials, `assembleRelease` still works and produces an unsigned APK.
- The key must never be lost or replaced: it is the only one that can sign updates (see [ADR-0007](0007-app-updates-and-data-compatibility.md)).
- Release builds use R8 (`isMinifyEnabled`, `isShrinkResources`).

## Alternatives considered

- **Newest AGP/Gradle (9.5.1 / 9.9.1):** works "with deprecation warnings at best" according to Kotlin's compatibility table; rejected to stay inside supported ranges.
- **`buildSrc` instead of an included `build-logic`:** any change in `buildSrc` invalidates the whole build; included builds are the current Gradle recommendation.
- **Signing config in `gradle.properties`:** easy to commit by mistake; rejected.

## Consequences

- Versions can be bumped in one file; the compatibility table above must be re-checked on every Kotlin/AGP/Gradle bump.
- `.devcontainer/scripts/android_sdk.sh` pins the Android platform and build-tools; keep them in sync with the catalog.
- Every module gets the same targets, compiler options and quality checks for free.
