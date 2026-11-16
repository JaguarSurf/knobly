# ADR-0011: Network access for optional features

- **Status:** accepted
- **Date:** 2026-11-16
- **Supersedes:** [ADR-0003](0003-offline-first-no-internet.md), which ruled out the `INTERNET` permission

## Context

ADR-0003 kept Knobly away from the network: the manifest removed the `INTERNET` permission even if a library declared it, CI failed if the APK requested it, and the README promised "no internet permission".

That promise closes the door on useful features that need the network, for example downloading an updated list of known spam or telemarketing numbers. Knobly is a utilities platform, and more features are planned. The privacy goals of ADR-0003 still hold; only the blanket ban on the permission is in the way.

## Decision

- Knobly **may declare the `INTERNET` permission** when a feature needs it. The manifest no longer removes it and CI no longer checks for it.
- The privacy rules of ADR-0003 stay:
  - **No ads, analytics, telemetry or remote crash reporting** SDKs.
  - **The user's data stays on the device**: contacts, call history and filters are never sent anywhere. `android:allowBackup` stays `false` ([ADR-0007](0007-app-updates-and-data-compatibility.md)).
- Every feature that uses the network:
  - needs **its own ADR**, saying what it downloads or sends, to where, and why;
  - must be **optional**: the rest of the app keeps working offline;
  - must tell the user, in the app, what it does with the network.

## Alternatives considered

- **Keep ADR-0003:** the strongest and easiest privacy promise to verify, but it blocks every networked feature, even ones that only download public data.
- **Allow the network without rules:** the most flexible, but it would make the privacy promise vague. Rejected: the rules above keep the promise concrete.

## Consequences

- Features such as a downloadable number list become possible, each one with its own ADR.
- The README and the store page can no longer say "no internet permission". They promise what still holds: no ads or tracking, and the user's data stays on the phone.
- Nothing stops a library from adding the permission silently anymore. When adding a dependency, check its merged manifest (`aapt2 dump permissions <apk>`).
- Today the app still declares no `INTERNET` permission: no current dependency adds it.
