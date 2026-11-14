package app.knobly.domain.rules.call

import app.knobly.core.model.Action
import app.knobly.core.model.IncomingCall
import app.knobly.core.model.RuleId
import app.knobly.domain.rules.Rule
import app.knobly.domain.rules.RuleContext

/**
 * Matches calls whose number starts with one of [prefixes] (e.g. `0303` telemarketing, `0304` donations).
 *
 * A prefix is compared with the received digits, the E.164 digits and the national number with and without the
 * trunk prefix `0`, because normalization drops it: `0303 123 4567` becomes national `3031234567`. So `0303`
 * matches it, and `011` matches every number from DDD 11.
 */
data class PrefixRule(
    override val id: RuleId,
    override val priority: Int,
    override val action: Action,
    val prefixes: List<String>,
    override val enabled: Boolean = true,
) : Rule<IncomingCall> {
    override fun matches(
        event: IncomingCall,
        context: RuleContext,
    ): Boolean {
        val number = event.number ?: return false
        val candidates =
            listOfNotNull(
                number.national,
                "0${number.national}",
                number.raw.removePrefix("+"),
                number.e164?.removePrefix("+"),
            )
        return prefixes.any { prefix -> candidates.any { it.startsWith(prefix) } }
    }
}

/** Matches calls with a hidden/withheld number. */
data class HiddenNumberRule(
    override val id: RuleId,
    override val priority: Int,
    override val action: Action,
    override val enabled: Boolean = true,
) : Rule<IncomingCall> {
    override fun matches(
        event: IncomingCall,
        context: RuleContext,
    ): Boolean = event.number == null
}

/**
 * Temporary, hard-coded rules for the technical spike, until rules are persisted and edited in the UI:
 * reject `0303` (telemarketing) and silence `0304` (donations).
 */
object SpikeRules {
    val calls: List<Rule<IncomingCall>> =
        listOf(
            PrefixRule(RuleId("spike-0303"), priority = 10, action = Action.REJECT, prefixes = listOf("0303")),
            PrefixRule(RuleId("spike-0304"), priority = 20, action = Action.SILENCE, prefixes = listOf("0304")),
        )
}
