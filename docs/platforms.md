# Android vs iOS: what is possible

The two platforms have very different capabilities for filtering calls. This is Knobly's main product constraint.

## Comparison

| Capability | Android | iOS |
|---|---|---|
| Real-time decision on every call | ✅ `CallScreeningService` | ❌ there is no per-call callback |
| "Contacts only" filter | ✅ by the app | ⚠️ only through the native "Silence Unknown Callers" feature |
| Regex | ✅ evaluated on the fly | ❌ only patterns that can be expanded into a list (e.g. prefixes) |
| Block list | ✅ | ✅ Call Directory Extension |
| Prefixes (0303, 0304) | ✅ | ⚠️ by expanding number ranges; extension memory limit |
| Reject, silence or allow | ✅ | ⚠️ block or identify only |
| History of filtered calls | ✅ | ❌ the app does not know when it blocked a call |
| Blocking statistics | ✅ | ❌ no data |
| Blocking overrides contacts and recents | ✅ per custom rule ("Aplicar também aos contatos"); checked on the Android 10 emulator: calls from contacts reach the `CallScreeningService` | ❌ contacts and recents always take precedence |

## Android

- **API:** `android.telecom.CallScreeningService` + `RoleManager.ROLE_CALL_SCREENING`.
- **Minimum:** Android 10 (API 29). Below that, only by being the default dialer, which is out of scope.
- **Possible responses:** `setDisallowCall`, `setRejectCall`, `setSilenceCall`, `setSkipCallLog`, `setSkipNotification`.
- **Permissions:** `READ_CONTACTS` (for the contacts filter), `READ_CALL_LOG` (optional: bring the incoming calls of the call log into History and tell whether calls that rang were answered or missed, see [ADR-0008](adr/0008-incoming-call-history-and-call-log.md) and [ADR-0009](adr/0009-bring-in-calls-from-the-call-log.md); a restricted permission on the Play Store that needs a declaration) and `POST_NOTIFICATIONS` (optional).
- **Calls from contacts:** the documentation indicates that the system may not forward calls from numbers in the contacts to the service. On the emulator (Android 10 and 17) they do reach it; still to confirm on Samsung, Xiaomi, Motorola and Pixel.

## iOS

- **API:** CallKit `CXCallDirectoryProvider` (Call Directory Extension) + App Group to share data.
- **How it works:** the app generates a sorted list of numbers (Int64, E.164 without `+`) and asks the system to reload the extension. iOS makes the decision when the call arrives.
- **Known instability:** there are reports on the Apple Developer Forums of intermittent blocking, or blocking that stopped working, on iOS 26 versions. Apple has also confirmed that contacts and recents of outgoing calls take precedence over blocks from third-party extensions, and that there is no public API to change this.
- **Native features the app should teach during onboarding:** "Silence Unknown Callers" and iOS's own unknown-caller screening.

## Strategy

1. **Android first**, with the full feature set. See [ADR-0002](adr/0002-android-first.md).
2. **iOS in a reduced version:** block list, prefixes and a guide to the native features, clearly communicated to the user.
3. The rule engine is shared: on iOS, it decides **which rules can become a list** and generates that list.
