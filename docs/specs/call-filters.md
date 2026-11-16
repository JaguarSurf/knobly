# Spec: call filters (Filters tab)

- **Status:** implemented on Android (iOS: see [platforms.md](../platforms.md))
- **Date:** 2026-11-14 (rules that also apply to contacts: 2026-11-16)

## Goal

Let the user choose which incoming calls are rejected, with toggles that can be switched on and off at any time. A rejected call must be **invisible**: for the user it is as if the call never happened.

## Filtering on/off

- Call filtering requires the call screening role (`ROLE_CALL_SCREENING`). While Knobly does not hold it, the Filters tab shows the "Call filtering is off" card (already implemented). The system's phone app always stays the default dialer.
- **All filters are off by default.** Holding the role alone changes nothing: every call rings until the user enables a filter.

## Filters

The Filters tab shows two toggles:

| Toggle | Rejects | Notes |
|---|---|---|
| **Contacts only** ("Aceitar somente contatos") | Every call whose number is not in the contacts, including hidden numbers | Needs `READ_CONTACTS` |
| **Custom rules** ("Regras personalizadas") | Calls matching any enabled custom rule | Group toggle + one toggle per rule |

Custom rule types:

| Type | Matches | Example |
|---|---|---|
| Starts with (prefix) | Numbers starting with the given digits; the trunk prefix `0` is optional (`0303` matches `+55 303…`) | `0303`, `0304`, `+1`, `11 4004` |
| Exact number | One specific number, also when saved or received without the country code, the DDD or the 9th digit ([phone-number-matching.md](phone-number-matching.md)) | `(11) 91234-5678` |
| Hidden number | Calls without caller id | — |
| Regular expression (advanced) | Regex over the number's digits (national, E.164 and as received); Android only | `^0800` |

Each rule can be paused (toggle), edited and deleted. Rules are not reordered.

**Apply to contacts too** ("Aplicar também aos contatos"): an option of each prefix, exact number and regex rule, off by default. When on, the rule also rejects numbers saved in the contacts (e.g. to block a saved number, or a whole prefix including saved numbers). It is not offered for hidden numbers, which are never contacts. In the rule list, such a rule shows a small person icon next to its name, announced by screen readers as "Também se aplica aos contatos". Stored as `applyToContacts` in `call_filters.json` (default `false`, so rules saved before it keep protecting contacts). Android only: on iOS contacts always take precedence ([platforms.md](../platforms.md)).

## How filters combine

- Filters **add up**: a call is rejected if **any** enabled filter matches.
- **Contacts pass**: a number saved in the contacts is not rejected by "contacts only" or by a custom rule, **unless that custom rule also applies to contacts**. Contacts saved without the 9th digit, the DDD or the country code count too ([phone-number-matching.md](phone-number-matching.md)).
- There is no user-visible priority. Internally the rule engine evaluates the custom rules that also apply to contacts first, then an allow rule for contacts, then the other reject rules; with a single action this is equivalent to the union of the filters. Future exceptions (e.g. "always allow") will also win over filters.
- A rule that applies to contacts works without the contacts permission too (it does not need to know who is a contact).

## Action: Reject

The only action for now. The call is answered by the `CallScreeningService` with `setDisallowCall(true)`, `setRejectCall(true)`, `setSkipCallLog(true)` and `setSkipNotification(true)`:

- the phone does not ring, show the call or notify (no "missed call");
- the call is hidden from the system call log as far as Android allows: the platform still records it with type `CallLog.Calls.BLOCKED_TYPE` regardless of `setSkipCallLog`, and whether the phone app shows blocked calls depends on the phone app;
- it **is** recorded in Knobly's own History tab, so the user can check what was blocked (see [call-history.md](call-history.md)).

Outside the app's control: the caller hears busy/voicemail depending on the carrier, and some manufacturers may behave differently (to be checked on real devices).

## Contacts permission

- Requested when the user turns on **Contacts only**, and also when turning on **Custom rules** (to keep contacts safe from them).
- Denied for Contacts only: the toggle goes back to off with an explanation and a shortcut to the app settings.
- Denied for Custom rules: rules still work, but a note says contacts cannot be protected from them.

## Data and behavior

- Toggles and rules are persisted as one JSON document (`CallFilterSettings`, through the `DocumentStore` of `:core:datastore`; each rule is `id + type + value + label + enabled`, see [ADR-0004](../adr/0004-generic-rule-engine.md)). `CallRulesProvider` keeps the engine rules built from it in memory for the `CallScreeningService`; changes apply to the next call immediately.
- Contacts are matched in memory by `ContactDirectory`, an index of the saved numbers built from the `ContactsAccess` port and rebuilt when a call finds that the contacts changed ([phone-number-matching.md](phone-number-matching.md)). Without the permission the lookup is unavailable and **fails open**: "contacts only" does nothing instead of rejecting every call.
- `CallScreener` answers within 3 seconds at most: if reading the settings or the contacts takes longer, the call rings (fail open).
- Regex rules are validated when saved (invalid patterns are rejected) and compiled once; pattern length is limited to keep matching fast inside the service.
- The hard-coded spike rules (`SpikeRules`, 0303/0304) were removed; with every filter off, every call rings.

## UI language

pt-BR first, English as fallback (Compose Multiplatform resources). Code and identifiers stay in English.

## Out of scope for now

Actions other than Reject (silence, notify), presets, "always allow" list, schedules, repeat callers, temporary release, neighbor spoofing.
