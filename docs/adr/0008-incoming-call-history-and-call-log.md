# ADR-0008: Record every incoming call, and read the call log (optional) for outcomes

- **Status:** accepted; partly changed by [ADR-0009](0009-bring-in-calls-from-the-call-log.md) (the call log is now also used to bring in the calls Knobly did not screen)
- **Date:** 2026-11-14
- **Changes:** [platforms.md](../platforms.md), which said Knobly would not use `READ_CALL_LOG`.

## Context

History and Statistics only showed calls rejected by Knobly. Users also want to see the calls that went through: answered or missed, and totals that include them.

Knobly's `CallScreeningService` is consulted for every incoming call (as long as it holds the call screening role), so it can record the calls it lets ring. But it never learns how they ended: only the system call log knows whether a call was answered, missed, declined or sent to voicemail. Reading it needs `READ_CALL_LOG`, a sensitive permission that Google Play only allows for some app categories (call screening and caller ID apps are one of them, with a declaration).

## Decision

- Knobly **records every incoming call it screens**: rejected (as before) and allowed ("rang"). Outgoing calls are out of scope. Same retention (3 years), same database (`call_events`, schema version 2 adds `outcome`).
- `READ_CALL_LOG` is **optional**. Without it, allowed calls stay "Recebida" (rang, outcome unknown) and History and Statistics offer to allow it. With it, `CallOutcomeResolver` matches each call that rang with its call log entry (same number with the [matching rules](../specs/phone-number-matching.md), started within 2 minutes) and stores **answered** or **missed** (missed, declined or voicemail). It runs after each screened call and when History or Statistics open; calls older than 30 days are no longer looked for.
- The call log is **only read**, only for those calls, and nothing is copied from it except the outcome. Nothing leaves the device (ADR-0003).
- History shows the **contact's name** for saved numbers, looked up when the screen is shown (not stored).

## Alternatives considered

- **Only what Knobly sees** (no call log): simpler, no sensitive permission, but answered and missed cannot be told apart.
- **Mirror the whole call log**: would include calls from before Knobly was installed and outgoing calls, but makes Knobly a second call log app and reads much more than needed.
- **Phone state listener** (`READ_PHONE_STATE`) to observe answered calls: fragile across Android versions and still needs a sensitive permission.

## Consequences

- Google Play: the release needs the "Permissions declaration form" for `READ_CALL_LOG`, under the call screening / caller ID use; F-Droid has no such restriction. If Play refuses it, the permission can be removed: the app keeps working with "Recebida" for calls that rang.
- Knobly now stores the numbers of all incoming calls (also contacts), like the system call log does; still only on the device, deletable from History, and pruned after 3 years.
- First database migration (version 1 → 2, automatic, with a migration test).
- iOS: neither the screening callback nor the call log exist; History stays empty there.
