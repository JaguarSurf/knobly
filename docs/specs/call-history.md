# Spec: call history (History tab)

- **Status:** implemented on Android (iOS: see [platforms.md](../platforms.md))
- **Date:** 2026-11-14 (calls from the call log: 2026-11-15; no retention limit: 2026-11-16)

## Goal

Show the user every incoming call and how it ended: **answered**, **missed** or **rejected** by Knobly, so filters can be trusted and adjusted. Rejected calls never ring or notify (see [call-filters.md](call-filters.md)); the History tab is where they become visible. Decision and alternatives: [ADR-0008](../adr/0008-incoming-call-history-and-call-log.md) and [ADR-0009](../adr/0009-bring-in-calls-from-the-call-log.md).

## What is recorded

- **Every incoming call Knobly screens**, and, with the call log permission, **every incoming call of the system call log** Knobly did not screen (outgoing calls are not). Each entry keeps: time, number (`raw`, E.164 and national forms; nothing when the number was hidden) and how it ended:

  | Outcome | Meaning | Shown as |
  |---|---|---|
  | Rejected | Rejected by a filter, or blocked by the system or another app; it never rang | "Rejeitada · <reason>" |
  | Answered | Rang and was answered (here or on another device) | "Atendida" |
  | Missed | Rang and was not answered: missed, declined or sent to voicemail | "Perdida" |
  | Received | Rang; how it ended is not known (no call log access, or not in the call log yet) | "Recebida" |

- **The system call log is synced** (`READ_CALL_LOG`, optional): after each screened call, when History or Statistics open and right after the permission is granted. Each incoming call log entry that the history does not know yet (the whole call log) is:
  - **matched** with the call Knobly screened (same number, with the [matching rules](phone-number-matching.md), started within 2 minutes; a blocked entry with a rejected call, an answered or missed one with a call that rang): a call that rang becomes **answered** or **missed**;
  - otherwise **brought in** as a new entry: calls from before Knobly was installed, from while it did not hold the call screening role, and calls **blocked by the system or another app** ("Rejeitada · Bloqueada pelo sistema").
- Entries are recognized by start time and number, so syncing again never duplicates them. Entries the user deleted (one, or the whole history) are not brought in again. The system call log is only read, never changed.
- Without the permission, History shows only the calls Knobly screened, and those that rang stay "Recebida".
- Rejected calls also keep the reason and a **snapshot of the rule** that rejected them (type, value, label). The snapshot keeps the history readable after the rule is edited or deleted.
- Reasons: **contacts only** ("Fora dos contatos"), a **custom rule** ("Telemarketing · começa com 0303"), **unknown** ("Regra excluída", when the rule was deleted between screening and recording) or **blocked by the system** ("Bloqueada pelo sistema", calls brought in from the call log).
- The entry is written **after** the call screening has answered, never before (the answer must be fast). The write runs on the process scope, so it survives the service being unbound.

## Retention and privacy

- Kept **only on the device** (Room database `knobly.db`), like everything else in Knobly. No backup or export yet.
- **No retention limit:** calls are kept until the user deletes them; nothing is deleted automatically ([ADR-0010](../adr/0010-keep-the-call-history-without-a-retention-limit.md)). Every year feeds the yearly statistics ([call-statistics.md](call-statistics.md)).
- The user can delete one entry or **clear the whole history** (with a confirmation). Deleted calls are not brought in again from the call log.

## History tab

Follows the approved mockup:

- Header "Histórico" with a short explanation and a "Limpar histórico" button (only when there are entries).
- Filter chips: **Todas**, **Atendidas**, **Perdidas** and **Rejeitadas**.
- While the call log cannot be read (also on an empty history), a card offers to allow it ("Trazer o registro de chamadas?"), to bring in the calls Knobly did not screen and tell answered from missed; if the user refuses, it points to the app settings. Not shown on iOS.
- Entries grouped by **local day**: "Hoje", "Ontem", then the date ("9 de novembro"; with the year when it is not the current one). Newest first. Each row shows an icon per outcome, the **contact's name** when the number is saved (looked up when shown, never stored; numbers match with or without the 9th digit and DDD) or the formatted number (or "Número oculto"), the outcome ("Atendida · (11) 91234-5678", "Rejeitada · Fora dos contatos") and the local time (`HH:mm`).
- Numbers are formatted for display: national format for Brazilian numbers (`(11) 91234-5678`, `0303 123 4567`, also `0304 765 4321`, which libphonenumber does not accept as valid), international format for other countries.
- Tapping an entry opens a **details sheet**: name or number, full date and time, status (outcome and, for rejections, the reason; "Do registro de chamadas do aparelho" for calls brought in from the call log), **Copiar número** (not for hidden numbers) and **Excluir registro**.
- Empty states: no calls yet; no calls for the selected filter.
- Footer: "O histórico fica só neste aparelho e é guardado até você apagá-lo."

## Out of scope (for now)

- Actions that change filters from an entry ("always allow this number", "block this number"): they come with the allow list.
- Pagination: a year of rejected calls is expected to stay in the low thousands; the list is loaded at once.
- iOS: the system does not tell the app when a call was blocked, so the history stays empty there.
