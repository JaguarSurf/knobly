# Spec: call history (History tab)

- **Status:** implemented on Android (iOS: see [platforms.md](../platforms.md))
- **Date:** 2026-11-14

## Goal

Show the user which calls Knobly rejected, so filters can be trusted and adjusted. Rejected calls never ring or notify (see [call-filters.md](call-filters.md)); the History tab is where they become visible.

## What is recorded

- **Only rejected calls.** Allowed calls are not recorded: the system call log already has them.
- Each entry keeps: time, number (`raw`, E.164 and national forms; nothing when the number was hidden), the reason and a **snapshot of the rule** that rejected it (type, value, label). The snapshot keeps the history readable after the rule is edited or deleted.
- Reasons: **contacts only** ("Fora dos contatos"), a **custom rule** ("Telemarketing · começa com 0303"), or **unknown** ("Regra excluída", when the rule was deleted between screening and recording).
- The entry is written **after** the call screening has answered, never before (the answer must be fast). The write runs on the process scope, so it survives the service being unbound.

## Retention and privacy

- Kept **only on the device** (Room database `knobly.db`), like everything else in Knobly. No backup or export yet.
- Entries older than **3 years** are deleted automatically whenever a new call is recorded. Three years feed the yearly statistics ([call-statistics.md](call-statistics.md)).
- The user can delete one entry or **clear the whole history** (with a confirmation).

## History tab

Follows the approved mockup:

- Header "Histórico" with a short explanation and a "Limpar histórico" button (only when there are entries).
- Filter chips: **Todas**, **Fora dos contatos** (contacts only) and **Regras** (custom rules, including deleted ones).
- Entries grouped by **local day**: "Hoje", "Ontem", then the date ("9 de novembro"; with the year when it is not the current one). Newest first. Each row shows the formatted number (or "Número oculto"), the reason and the local time (`HH:mm`).
- Numbers are formatted for display: national format for Brazilian numbers (`(11) 91234-5678`, `0303 123 4567`, also `0304 765 4321`, which libphonenumber does not accept as valid), international format for other countries.
- Tapping an entry opens a **details sheet**: number, full date and time, reason, **Copiar número** (not for hidden numbers) and **Excluir registro**.
- Empty states: no rejected calls yet; no calls for the selected filter.
- Footer: "O histórico fica só neste aparelho e é guardado por 3 anos."

## Out of scope (for now)

- Actions that change filters from an entry ("always allow this number", "block this number"): they come with the allow list.
- Pagination: a year of rejected calls is expected to stay in the low thousands; the list is loaded at once.
- iOS: the system does not tell the app when a call was blocked, so the history stays empty there.
