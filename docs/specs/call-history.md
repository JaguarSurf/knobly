# Spec: call history (History tab)

- **Status:** implemented on Android (iOS: see [platforms.md](../platforms.md))
- **Date:** 2026-11-14

## Goal

Show the user every incoming call and how it ended: **answered**, **missed** or **rejected** by Knobly, so filters can be trusted and adjusted. Rejected calls never ring or notify (see [call-filters.md](call-filters.md)); the History tab is where they become visible. Decision and alternatives: [ADR-0008](../adr/0008-incoming-call-history-and-call-log.md).

## What is recorded

- **Every incoming call Knobly screens** (outgoing calls are not). Each entry keeps: time, number (`raw`, E.164 and national forms; nothing when the number was hidden) and how it ended:

  | Outcome | Meaning | Shown as |
  |---|---|---|
  | Rejected | Rejected by a filter; it never rang | "Rejeitada · <reason>" |
  | Answered | Rang and was answered (here or on another device) | "Atendida" |
  | Missed | Rang and was not answered: missed, declined or sent to voicemail | "Perdida" |
  | Received | Rang; how it ended is not known (no call log access, or not in the call log yet) | "Recebida" |

- **Answered or missed comes from the system call log** (`READ_CALL_LOG`, optional). After each screened call, and when History or Statistics open, the calls that rang are matched with their call log entries (same number, with the [matching rules](phone-number-matching.md), started within 2 minutes). Calls older than 30 days are no longer looked for. Without the permission they stay "Recebida".
- Rejected calls also keep the reason and a **snapshot of the rule** that rejected them (type, value, label). The snapshot keeps the history readable after the rule is edited or deleted.
- Reasons: **contacts only** ("Fora dos contatos"), a **custom rule** ("Telemarketing · começa com 0303"), or **unknown** ("Regra excluída", when the rule was deleted between screening and recording).
- The entry is written **after** the call screening has answered, never before (the answer must be fast). The write runs on the process scope, so it survives the service being unbound.

## Retention and privacy

- Kept **only on the device** (Room database `knobly.db`), like everything else in Knobly. No backup or export yet.
- Entries older than **3 years** are deleted automatically whenever a new call is recorded. Three years feed the yearly statistics ([call-statistics.md](call-statistics.md)).
- The user can delete one entry or **clear the whole history** (with a confirmation).

## History tab

Follows the approved mockup:

- Header "Histórico" with a short explanation and a "Limpar histórico" button (only when there are entries).
- Filter chips: **Todas**, **Atendidas**, **Perdidas** and **Rejeitadas**.
- When some calls are "Recebida" and the call log cannot be read, a card offers to allow it ("Atendidas ou perdidas?"); if the user refuses, it points to the app settings.
- Entries grouped by **local day**: "Hoje", "Ontem", then the date ("9 de novembro"; with the year when it is not the current one). Newest first. Each row shows an icon per outcome, the **contact's name** when the number is saved (looked up when shown, never stored; numbers match with or without the 9th digit and DDD) or the formatted number (or "Número oculto"), the outcome ("Atendida · (11) 91234-5678", "Rejeitada · Fora dos contatos") and the local time (`HH:mm`).
- Numbers are formatted for display: national format for Brazilian numbers (`(11) 91234-5678`, `0303 123 4567`, also `0304 765 4321`, which libphonenumber does not accept as valid), international format for other countries.
- Tapping an entry opens a **details sheet**: name or number, full date and time, status (outcome and, for rejections, the reason), **Copiar número** (not for hidden numbers) and **Excluir registro**.
- Empty states: no calls yet; no calls for the selected filter.
- Footer: "O histórico fica só neste aparelho e é guardado por 3 anos."

## Out of scope (for now)

- Actions that change filters from an entry ("always allow this number", "block this number"): they come with the allow list.
- Pagination: a year of rejected calls is expected to stay in the low thousands; the list is loaded at once.
- iOS: the system does not tell the app when a call was blocked, so the history stays empty there.
