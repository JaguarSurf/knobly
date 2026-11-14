# ADR-0001: Kotlin Multiplatform + Compose Multiplatform

- **Status:** accepted
- **Date:** 2026-11-13

## Context

Knobly needs to run on Android and iOS with organized, reusable code. The most critical piece is the **rule engine**, which on Android runs inside the `CallScreeningService`: in the background, woken up by the system, with a few seconds to respond.

## Decision

Use **Kotlin Multiplatform** for the domain, data and rules, and **Compose Multiplatform** for the shared UI.

## Alternatives considered

- **Flutter + Pigeon:** fast UI development, but the call screening service would have to start a Dart engine in the background (heavy and slow) or have the rule engine rewritten in Kotlin, duplicating the most critical logic.
- **React Native:** the same problem as Flutter, with a JS runtime in the background.
- **Pure native (Kotlin + Swift):** maximum control, but duplicates the entire UI and domain.

## Consequences

- The rule engine is written once and runs natively in the Android service and in the iOS app.
- The UI is shared; platform-specific code is isolated in `:platform:*` and in `expect/actual`.
- The team needs to master Kotlin; integrating with iOS APIs (CallKit) requires a bit of Swift in the extension.
- Re-evaluated on 2026-11-13 against Flutter, React Native and pure native with "full creative freedom and high performance" as criteria: on Android, Compose Multiplatform *is* Jetpack Compose (the native toolkit), and only KMP runs the rule engine natively inside the `CallScreeningService` without a bridge or duplicated logic. Confirmed.
- The iOS UI choice (shared Compose Multiplatform vs. SwiftUI on top of the shared KMP domain) is deferred to the iOS release; both work with this architecture.
- A JVM desktop target is added for development and testing; see [ADR-0005](0005-desktop-harness-and-platform-ports.md).
