package app.knobly.domain.history

import app.knobly.core.model.PhoneNumber
import app.knobly.domain.rules.call.CustomRuleType
import kotlin.time.Instant

/** A call rejected by Knobly, as shown in the History tab. */
data class RejectedCall(
    val id: Long,
    val timestamp: Instant,
    /** `null` when the caller's number was hidden. */
    val number: PhoneNumber?,
    val reason: RejectionReason,
    /** The custom rule that rejected the call, as it was at that moment ([RejectionReason.CUSTOM_RULE] only). */
    val rule: RuleSnapshot? = null,
)

enum class RejectionReason {
    /** "Aceitar somente contatos": the caller is not in the contacts (or the number was hidden). */
    CONTACTS_ONLY,

    /** One of the user's custom rules. */
    CUSTOM_RULE,

    /** A rule that no longer exists in the settings by the time the call was recorded. */
    UNKNOWN,
}

/** The parts of a custom rule needed to explain a rejection, copied when the call is recorded. */
data class RuleSnapshot(
    val id: String,
    val type: CustomRuleType,
    val value: String,
    val label: String,
)
