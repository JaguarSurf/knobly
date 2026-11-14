# ADR-0002: Android first, iOS in a reduced version

- **Status:** proposed
- **Date:** 2026-11-13

## Context

Android provides the `CallScreeningService`, which makes it possible to decide on every call and record the result. iOS only provides the Call Directory Extension: a static list, no callback, no log, contacts taking precedence, and reports of instability on iOS 26. See [platforms.md](../platforms.md).

## Decision

Ship the **MVP on Android** with all features (filters, history, statistics). iOS comes later, with reduced scope: block list, prefixes and a guide to the native features.

## Alternatives considered

- **Parity between platforms:** impossible with the current iOS APIs.
- **Ship both together with the lowest common denominator:** would make Android poorer with no real gain on iOS.

## Consequences

- The MVP ships faster and with the complete value proposition.
- Communication must be clear: the iOS version does less, and the reason is an Apple limitation.
- The history and statistics UI on iOS needs an explanatory empty state.
