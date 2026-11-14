# Spec: phone number matching

- **Status:** implemented (`PhoneNumberMatching` in `:core:phone`, `ContactDirectory` in `:domain:rules`)
- **Date:** 2026-11-14

## Goal

Recognize a caller as the same number the user saved, however it was saved. In Brazil, contact lists often hold numbers from before 2016 without the **9th mobile digit**, or without the **DDD** (area code) or the **country code**. Filters must keep working in all these cases.

## Where it applies

| Filter | Uses this matching |
|---|---|
| "Aceitar somente contatos" | Yes: is the caller in the contacts? |
| "Contatos sempre passam" (custom rules never reject a contact) | Yes |
| Custom rule **Número exato** | Yes: the saved number and the caller are compared with the same rules |
| Custom rules **Começa com** and **Regex** | No: they match the digits as written (see [call-filters.md](call-filters.md)) |
| History: contact names, and matching a call with its call log entry | Yes ([call-history.md](call-history.md)) |

## Rules

Numbers are first normalized (`LibPhoneNumberNormalizer`): formatting, the trunk prefix `0`, carrier codes (`0 15 …`) and `+55` are removed, which leaves the national digits: DDD + number, e.g. `35 9 8123-4567`.

Two **Brazilian** numbers (with `+55`, or saved without a country code) are the same when:

1. **The last 8 digits are equal** (`8123-4567`). Fewer than 8 digits on either side (short codes such as `190`): the numbers must be identical.
2. **The DDD is equal, if both numbers have one.** A number saved without a DDD matches the same 8 digits in any DDD.
3. **The 9th digit may be missing on either side** when the 8 digits start with **6, 7, 8 or 9**: those are the mobile numbers that received the extra 9 (2012–2016). Landlines start with 2 to 5 and never had it, so for them the 9 must be on both sides or on neither.
4. Anything else before the last 8 digits (an unknown prefix) means only identical numbers match.

Numbers from **other countries** must be identical (same E.164 number).

## Examples

Caller: **+55 (35) 98123-4567**

| Saved as | Same number? | Why |
|---|---|---|
| `+55 (35) 98123-4567`, `(35) 98123-4567`, `035 98123-4567`, `0 15 35 98123-4567` | Yes | Same number, other formats |
| `(35) 8123-4567`, `+55 35 8123-4567` | Yes | Without the 9th digit (mobile starting with 8) |
| `98123-4567`, `8123-4567` | Yes | Without the DDD (and the country code) |
| `(11) 98123-4567`, `(11) 8123-4567` | No | Another DDD |
| `(35) 98123-4568` | No | Another number |

Landline caller **(35) 3123-4567**: `3123-4567` matches; `(35) 93123-4567` does not (a landline never had a 9th digit).

## Trade-off

A number saved without a DDD matches every DDD with the same 8 digits. For contacts this is the safe side: at worst, an unknown caller with the same 8 digits rings. For a **Número exato** rule saved without a DDD, the same tolerance means it rejects that number in any DDD; save the DDD to restrict it.

## Implementation

- `PhoneNumberMatching.sameNumber(a, b)` (`:core:phone`, pure Kotlin) implements the rules; tests: `PhoneNumberMatchingTest` (real normalizer) and `CallFiltersTest`.
- `ContactDirectory` (`:domain:rules`) keeps the contacts' numbers in memory, indexed by their last 8 digits, so a call does not read every contact while the phone rings. It is built in the background as soon as the contacts can be read.
- On every call, a cheap **contacts version** (Android: the latest `CONTACT_LAST_UPDATED_TIMESTAMP` and `CONTACT_DELETED_TIMESTAMP`, two one-row queries) tells whether contacts were added, edited or deleted since; if so, the index is rebuilt before matching. Change notifications are not used: Android freezes the app's process between calls and delivers them too late (seen on the emulator: a contact added between two calls was missed).
- The `ContactsAccess` port only reads the saved numbers (Android: `ContactsContract.CommonDataKinds.Phone`) and that version; it no longer uses `PhoneLookup`, whose own matching does not tolerate a missing 9th digit.
- Emulator timings (debug build): building the index on the first call ~0.3 s; a call with an unchanged index ~10–20 ms; a call right after a contact change ~30 ms.
- Text after the number (extensions such as `ramal 12`, pauses `,` and waits `;`) is ignored.
