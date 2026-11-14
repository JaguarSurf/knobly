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

> To be defined once the KMP project is created (build, tests, lint, run on a device).

## Current status

Phase: **planning / technical spike**. See [roadmap.md](docs/roadmap.md).
