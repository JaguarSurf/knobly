# ADR-0009: Bring incoming calls from the system call log into the history

- **Status:** accepted
- **Date:** 2026-11-15
- **Changes:** [ADR-0008](0008-incoming-call-history-and-call-log.md), which read the call log only for the outcome of the calls Knobly screened and rejected mirroring it.

## Context

History only showed the calls Knobly screened. Calls from before Knobly was installed, calls received while it did not hold the call screening role, and calls blocked by the system or another app were missing, so History and Statistics started empty and had gaps. Users asked to bring their call history into the app. Knobly already holds the optional `READ_CALL_LOG` permission (ADR-0008) for outcomes.

## Decision

- With `READ_CALL_LOG` granted, `CallLogSync` (`:domain:history`, replaces `CallOutcomeResolver`) **syncs continuously**: when History or Statistics open, after each screened call and right after the permission is granted.
- Only **incoming** calls: answered, missed (missed, declined, voicemail) and **blocked** (`BLOCKED_TYPE`). Outgoing calls stay out of scope.
- Each call log entry the history does not know yet is either:
  - **linked** to the call Knobly screened (same number with the [matching rules](../specs/phone-number-matching.md), started within 2 minutes, same kind: blocked ↔ rejected, answered/missed ↔ rang). A call that rang gets its outcome. This also links Knobly's own rejections that Android logs as blocked despite `setSkipCallLog`;
  - or **brought in** as a new entry with `source = call_log`. Blocked entries become "Rejeitada · Bloqueada pelo sistema" (`reason = system_blocked`), a separate category in Statistics.
- Entries are identified by a **key** of start time (ms) and number, not by the call log row id, so a restored call log is not brought in twice.
- Calls the user **deletes or clears** from History are remembered (table `ignored_call_log_entries`) and not brought in again. The Android call log is never written to.
- Only the last **3 years** (the history retention) are read. Imported calls look like the others; only the details sheet says "Do registro de chamadas do aparelho". They count in Statistics and Home.
- Schema version 3 (automatic migration, with a migration test): `call_events.source` (default `knobly`), `call_events.call_log_key` (indexed) and the `ignored_call_log_entries` table.

## Alternatives considered

- **One-time manual import**: simpler to explain, but calls Knobly misses later (role lost, app killed by the manufacturer) would leave gaps again.
- **Import outgoing calls too**: makes Knobly a second call log app and needs another outcome and Statistics changes; not related to filtering.
- **Mark imported calls in the list**: more noise for little value; the details sheet is enough.
- **Watermark instead of keys** ("everything after time T is synced"): breaks when entries are written late or the call log is restored.

## Consequences

- Knobly stores a copy of the incoming part of the call log (number, time, type), still only on the device (ADR-0003), deletable from History and pruned after 3 years.
- The Play Store `READ_CALL_LOG` declaration should mention that the call history is shown in the app.
- Each sync reads up to 3 years of incoming calls (Android keeps a few hundred to a few thousand entries), off the main thread.
- iOS: no call log; `CallLogAccess.isAvailable` is false and the offer is not shown.
