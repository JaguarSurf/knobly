# ADR-0003: Offline and no internet permission

- **Status:** superseded by [ADR-0011](0011-network-access-for-optional-features.md)
- **Date:** 2026-11-13

## Context

Knobly handles sensitive data (who calls the user, the contact list). The product goal is to be free and trustworthy, in contrast with competing apps full of ads and data collection.

## Decision

The MVP **does not declare the `INTERNET` permission**. It uses no ad, analytics, remote crash reporting or telemetry SDKs. All data stays on the device.

## Alternatives considered

- **Anonymous analytics:** would help prioritize features, but breaks the privacy promise and complicates LGPD (Brazil's data protection law) compliance.
- **Remote crash reporting (e.g. Crashlytics):** useful, but requires internet access and sending data. Replaced by voluntary feedback and the app stores' own reports.

## Consequences

- LGPD compliance is practically guaranteed by design.
- A strong marketing argument, verifiable by the user on the store page.
- Less visibility into production bugs: compensate with tests, beta testers and the stores' own crash reports.
- Any future feature that needs the network (e.g. a community list) requires a new ADR and must be optional.
