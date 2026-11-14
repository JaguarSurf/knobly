package app.knobly.domain.rules

import app.knobly.core.model.Action
import app.knobly.core.model.Event
import app.knobly.core.model.PhoneNumber
import app.knobly.core.model.RuleId

/**
 * A filter rule (Strategy). Each rule type is one implementation; adding a type never changes [RuleEngine].
 *
 * [matches] runs inside the Android `CallScreeningService`, which must answer within a few seconds:
 * it must be fast, synchronous and free of I/O (use [RuleContext] for anything that needs data).
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

/** Data a rule may need while matching, served from in-memory caches. */
data class RuleContext(
    val contacts: ContactLookup = ContactLookup.None,
)

/** In-memory view of the user's contacts. */
fun interface ContactLookup {
    fun isContact(number: PhoneNumber): Boolean

    companion object {
        val None = ContactLookup { false }
    }
}
