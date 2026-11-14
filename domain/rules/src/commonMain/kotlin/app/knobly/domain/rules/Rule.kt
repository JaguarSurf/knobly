package app.knobly.domain.rules

import app.knobly.core.model.Action
import app.knobly.core.model.Event
import app.knobly.core.model.PhoneNumber
import app.knobly.core.model.RuleId

/**
 * A filter rule (Strategy). Each rule type is one implementation; adding a type never changes [RuleEngine].
 *
 * [matches] runs while the Android `CallScreeningService` waits for an answer: it must be fast and must not touch
 * the database (rules are prepared in memory; see [app.knobly.domain.rules.call.CallRulesProvider]).
 */
interface Rule<in E : Event> {
    val id: RuleId
    val priority: Int
    val action: Action
    val enabled: Boolean

    fun matches(
        event: E,
        context: RuleContext,
    ): Boolean
}

/** Data a rule may need while matching. */
data class RuleContext(
    val contacts: ContactLookup = ContactLookup.Unavailable,
)

/** Whether a number is saved in the user's contacts. */
interface ContactLookup {
    /** `false` when Knobly cannot read the contacts (e.g. permission revoked): contact-based rules then do nothing. */
    val isAvailable: Boolean

    /** May query the system contacts: never call on the main thread. */
    fun isContact(number: PhoneNumber): Boolean

    companion object {
        val Unavailable: ContactLookup =
            object : ContactLookup {
                override val isAvailable = false

                override fun isContact(number: PhoneNumber) = false
            }
    }
}
