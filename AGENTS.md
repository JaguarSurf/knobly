# Knobly — context for AI agents

This file is read automatically by coding agents (Claude Code reads it through `CLAUDE.md`) at the start of every session. Keep it up to date when decisions change.

## What Knobly is

A **100% free, ad-free and open source** mobile app for Android and iOS that gives users control over and customization of their own phone. The first feature is a **call filter** (abusive telemarketing, robocalls and scams are a serious problem in Brazil). The name is generic on purpose: the app is a **utilities platform**, and new features (SMS, notifications, other controls) will come later.

## Non-negotiable principles

1. **Free and ad-free.** No ad SDKs, no paywall.
2. **Privacy by design.** Everything runs and stays on the device. The MVP **does not declare the `INTERNET` permission**. No analytics, no telemetry. See [ADR-0003](docs/adr/0003-offline-first-no-internet.md).
3. **Organized, modular, reusable code that is easy to evolve.** Clear separation of responsibilities (Clean Architecture). See [architecture.md](docs/architecture.md).
4. **Honesty about platform limits.** iOS does not allow what Android allows; do not promise parity. See [platforms.md](docs/platforms.md).

## Git usage

Agents may use Git commands only to read repository state, such as `git status`, `git diff`, `git log`, and `git show`. Do not run `git add` or any other Git command that changes files, the index, staging status, commits, branches, or other repository state. Leave all staging and other Git mutations to the user.

## Stack

- **Kotlin Multiplatform (KMP)** + **Compose Multiplatform** (shared UI). See [ADR-0001](docs/adr/0001-kotlin-multiplatform.md).
- Targets: **Android**, **iOS** and **JVM desktop** (the desktop target is a development harness only, never shipped). See [ADR-0005](docs/adr/0005-desktop-harness-and-platform-ports.md).
- Navigation Compose (KMP), ViewModel (KMP), Coroutines/Flow
- Room (KMP) for persistence, DataStore (KMP) for preferences
- Koin for dependency injection
- kotlinx-datetime, kotlinx-serialization
- libphonenumber (Android and JVM) / PhoneNumberKit (iOS) via `expect/actual` for number normalization
- Vico (multiplatform) for charts
- WorkManager (Android) / BGTaskScheduler (iOS) for periodic tasks
- Compose Hot Reload for fast UI iteration on the desktop harness
- Tests: kotlin.test, Turbine, Compose UI tests (`runComposeUiTest`), Roborazzi (screenshot tests), Robolectric
- Quality: Detekt, ktlint. CI: GitHub Actions

## Architecture in one sentence

Apps → features → domain → core, with dependencies always pointing downwards. The **rule engine** (`:domain:rules`) is pure Kotlin, generic (it evaluates *events*, not only calls) and is called directly by the `CallScreeningService` on Android. Details in [architecture.md](docs/architecture.md).

## Conventions

- **Language:** **everything in the repository is written in English**: code, identifiers, comments, commit messages, branch names and documentation (README, `docs/**`, ADRs, this file). This applies to all future files too.
- **File names in `docs/`:** lowercase kebab-case (e.g. `docs/architecture.md`, `docs/adr/0005-desktop-harness-and-platform-ports.md`).
- **Base namespace/package:** `app.knobly` (adjust if the domain changes).
- **Commits:** Conventional Commits (`feat:`, `fix:`, `refactor:`, `docs:`, `test:`, `build:`, `ci:`, `chore:`).
- **New modules:** follow the `:core:*`, `:domain:*`, `:feature:*`, `:platform:*` pattern. A feature never depends on another feature.
- **Domain without platform dependencies:** no `android.*` or iOS APIs in `:domain:*` and `:core:model`.
- **Platform capabilities behind ports:** contacts, the call screening role, permissions and incoming calls are accessed only through interfaces (ports) defined in the domain. Real adapters live in `:platform:*`; fakes are adapters too and live in `:platform:fake`. `:feature:*` modules never call platform APIs directly, not even to request a permission.
- **Every shared library must support Android, iOS and JVM.** Otherwise the desktop harness and the JVM tests break.
- **UI text is never hard-coded:** use Compose resources in the module (`composeResources/values/strings.xml` in English as the fallback, `values-pt/strings.xml` in Portuguese) and `stringResource(Res.string.…)`. Every new string needs both languages. UI tests and screenshots run in pt-BR.
- **Visual identity:** UI follows [docs/design/visual-identity.md](docs/design/visual-identity.md) (dark "Noturno" with mint accent, light "Pedra", Poppins + Inter, "Fader k" icon). Feature behavior follows the specs in `docs/specs/`.
- **UI changes come with tests:** a Compose UI test and/or a Roborazzi screenshot for new or changed screens.
- **Every new filter rule** is an implementation of `Rule` + a UI form; do not change the engine to add a type.
- **Tests are mandatory** for the rule engine and for number normalization (Brazilian cases: +55, DDD area codes, 9th digit, hidden number, 0303/0304).
- **Relevant architectural decisions** become an ADR in `docs/adr/` (copy `docs/adr/0000-template.md`).

## Critical constraints the code must respect

- `CallScreeningService` must respond within a few seconds: **rules cached in memory**, the event is written to the database **after** responding.
- Android minimum: **API 29 (Android 10)**, because of `RoleManager.ROLE_CALL_SCREENING`.
- iOS: only the **Call Directory Extension** exists (static list). No per-call callback, no block log, contacts always take precedence.

## Testing without a smartphone

Use the cheapest layer that covers the change (details in [ADR-0005](docs/adr/0005-desktop-harness-and-platform-ports.md)):

1. **Desktop harness + fakes** (`:app:desktop`, X11 from the devcontainer, Hot Reload): screens, navigation, and the full rule → history → stats flow through the incoming call simulator.
2. **JVM tests** (UI tests + Roborazzi screenshots): run on every change and in CI. Agents can open the generated PNGs to review UI changes.
3. **Android emulator on demand** (KVM in the devcontainer): role request, permissions, `CallScreeningService` (`adb emu gsm call <number>`).
4. **Physical device:** before releases and for manufacturer-specific behavior.

## Commands

Always use the Gradle wrapper. Full guide: [development.md](docs/development.md).

```bash
# Build
./gradlew :app:android:assembleDebug        # -> app/android/build/outputs/apk/debug/knobly-debug.apk
./gradlew :app:android:assembleRelease      # R8; signed only with keystore.properties / KNOBLY_* env vars

# Tests and quality (run before handing work back)
./gradlew check                             # JVM + Android host tests, UI tests, Detekt, ktlint, Android lint
./gradlew ktlintFormat                      # fix formatting
./gradlew :app:shared:recordRoborazziJvm    # screenshots -> app/shared/build/outputs/roborazzi/*.png (open them to review UI)
./gradlew :core:designsystem:recordRoborazziJvm  # design system catalog, light + dark -> core/designsystem/build/outputs/roborazzi/

# Run without a phone
./gradlew :app:desktop:run                  # desktop harness (X11) with the incoming call simulator
./gradlew :app:desktop:hotRunJvm            # same, with Compose Hot Reload
scripts/emulator.sh                         # Android emulator (KVM), then: adb emu gsm call 03031234567

# Install on a phone (wireless debugging, see docs/development.md)
adb pair <ip>:<pairing-port> && adb connect <ip>:<port>
adb install -r app/android/build/outputs/apk/debug/knobly-debug.apk
adb logcat -s KnoblyScreening               # screening decisions and timing

# iOS (macOS only)
./gradlew iosSimulatorArm64Test
./gradlew :app:shared:linkDebugFrameworkIosSimulatorArm64
```

Notes for agents:

- Verify with the APK permissions (`aapt2 dump permissions <apk>`) that `android.permission.INTERNET` is never requested.
- Desktop/JVM UI tests need a test `Dispatchers.Main` (see `app/shared/src/jvmTest/.../TestHarness.kt`).
- Kotlin/Native (iOS) test names cannot contain `,` `.` `:` and similar characters, even in backticks.

## Current status

Phase: **technical spike**. The KMP skeleton builds an installable APK with the three tabs, an "Enable call filtering" card for `ROLE_CALL_SCREENING` in the Filters tab (no first-run screen) and a minimal `CallScreeningService` running hard-coded spike rules (`SpikeRules`). See [roadmap.md](docs/roadmap.md).
