# ADR-0010: Keep the call history without a retention limit

- **Status:** accepted
- **Date:** 2026-11-16
- **Changes:** [ADR-0008](0008-incoming-call-history-and-call-log.md) and [ADR-0009](0009-bring-in-calls-from-the-call-log.md), which kept calls for 3 years and only brought in call log entries of the last 3 years.

## Context

The history deleted calls older than 3 years whenever a new call was recorded, and the call log sync ignored older entries. Users want to keep their whole call history, like the system call log does, and decide themselves what to delete.

## Decision

- Calls are kept **until the user deletes them** (one entry, or the whole history). Nothing is deleted automatically.
- The call log sync reads the **whole** incoming call log, not only the last 3 years.
- The code that pruned old entries (`deleteOlderThan` in the repository and the DAO) is removed. The database schema does not change.

## Alternatives considered

- A longer limit (e.g. 10 years): still deletes data the user did not ask to delete, for little gain.
- A user setting for the retention (1 year, 3 years, forever): more UI and code for a need nobody has expressed yet; it can be added later on top of this decision.

## Consequences

- The history and the yearly statistics keep every year; the **Ano** view can show 5 full years.
- The database grows without a bound, but slowly: an incoming call is one small row, so even tens of thousands of calls take a few megabytes. Statistics still aggregate in memory (see [architecture.md](../architecture.md)); a pre-aggregated table can be added if the volume ever makes it slow.
- Privacy is unchanged: the history stays only on the device (ADR-0003) and can be deleted from the History tab at any time.
- Existing installs keep the calls they still have; calls already pruned before this change are not recovered from Knobly's database, but those still in the system call log are brought in again by the next sync.
