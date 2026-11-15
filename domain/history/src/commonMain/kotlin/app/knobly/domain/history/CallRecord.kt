package app.knobly.domain.history

import app.knobly.core.model.PhoneNumber
import app.knobly.domain.rules.call.CustomRuleType
import kotlin.time.Instant

/** An incoming call, as shown in History and counted in Statistics. */
data class CallRecord(
    val id: Long,
    val timestamp: Instant,
    /** `null` when the caller's number was hidden. */
    val number: PhoneNumber?,
    val outcome: CallOutcome,
    /** Why Knobly rejected it ([CallOutcome.REJECTED] only). */
    val reason: RejectionReason? = null,
    /** The custom rule that rejected the call, as it was at that moment ([RejectionReason.CUSTOM_RULE] only). */
    val rule: RuleSnapshot? = null,
    val source: CallSource = CallSource.KNOBLY,
    /** The system call log entry of this call ([app.knobly.domain.port.CallLogEntry.key]), once known. */
    val callLogKey: String? = null,
)

/** Where a call in the history comes from (docs/specs/call-history.md). */
enum class CallSource {
    /** Screened by Knobly's call screening service. */
    KNOBLY,

    /** Brought in from the system call log: Knobly did not screen it (before it was installed, or without the role). */
    CALL_LOG,
}

/** How an incoming call ended (docs/specs/call-history.md). */
enum class CallOutcome {
    /** Rejected (by Knobly, or by the system or another app): it never rang. */
    REJECTED,

    /** Allowed to ring; how it ended is not known (no call log access, or not in the call log yet). */
    RANG,

    /** Answered (on this phone or another device). */
    ANSWERED,

    /** Rang but was not answered: missed, declined or sent to voicemail. */
    MISSED,
}

enum class RejectionReason {
    /** "Aceitar somente contatos": the caller is not in the contacts (or the number was hidden). */
    CONTACTS_ONLY,

    /** One of the user's custom rules. */
    CUSTOM_RULE,

    /** A rule that no longer exists in the settings by the time the call was recorded. */
    UNKNOWN,

    /** Blocked by the system or another app, not by Knobly ([CallSource.CALL_LOG] only). */
    BLOCKED_BY_SYSTEM,
}

/** The parts of a custom rule needed to explain a rejection, copied when the call is recorded. */
data class RuleSnapshot(
    val id: String,
    val type: CustomRuleType,
    val value: String,
    val label: String,
)
