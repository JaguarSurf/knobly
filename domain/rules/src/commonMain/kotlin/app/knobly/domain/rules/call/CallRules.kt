package app.knobly.domain.rules.call

import app.knobly.core.model.Action
import app.knobly.core.model.IncomingCall
import app.knobly.core.model.PhoneNumber
import app.knobly.core.model.RuleId
import app.knobly.core.phone.PhoneNumberMatching
import app.knobly.domain.rules.Rule
import app.knobly.domain.rules.RuleContext

/**
 * Matches calls whose number starts with one of [prefixes] (e.g. `0303` telemarketing, `0304` donations).
 *
 * A prefix starting with `+` is a country code and is compared with the E.164 form only (`+1` never matches DDD 11).
 * Other prefixes are compared with the received digits and with the national number with and without the trunk
 * prefix `0`, because normalization drops it: `0303 123 4567` becomes national `3031234567`. So `0303` matches it,
 * and `011` matches every number from DDD 11.
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
        return prefixes.any { prefix ->
            if (prefix.startsWith("+")) {
                listOfNotNull(number.e164, number.raw.takeIf { it.startsWith("+") }).any { it.startsWith(prefix) }
            } else {
                listOf(number.national, "0${number.national}", number.raw).any { it.startsWith(prefix) }
            }
        }
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
 * Matches one specific number, also when it is saved or arrives without the country code, the DDD or the 9th digit
 * ([PhoneNumberMatching], docs/specs/phone-number-matching.md).
 */
data class ExactNumberRule(
    override val id: RuleId,
    override val priority: Int,
    override val action: Action,
    val target: PhoneNumber,
    override val enabled: Boolean = true,
) : Rule<IncomingCall> {
    override fun matches(
        event: IncomingCall,
        context: RuleContext,
    ): Boolean {
        val number = event.number ?: return false
        return PhoneNumberMatching.sameNumber(number, target)
    }
}

/**
 * Matches when [pattern] is found in any form of the number: as received (`+5511912345678`), E.164, national
 * (`11912345678`) and national with the trunk prefix (`011912345678`). Use `^`/`$` to anchor.
 */
class RegexRule(
    override val id: RuleId,
    override val priority: Int,
    override val action: Action,
    val pattern: Regex,
    override val enabled: Boolean = true,
) : Rule<IncomingCall> {
    override fun matches(
        event: IncomingCall,
        context: RuleContext,
    ): Boolean {
        val number = event.number ?: return false
        return listOfNotNull(number.raw, number.e164, number.national, "0${number.national}").any {
            pattern.containsMatchIn(it)
        }
    }
}

/** "Contacts always pass": allows any number saved in the contacts, before every reject rule. */
data class ContactAllowRule(
    override val id: RuleId,
    override val priority: Int,
    override val enabled: Boolean = true,
) : Rule<IncomingCall> {
    override val action: Action = Action.ALLOW

    override fun matches(
        event: IncomingCall,
        context: RuleContext,
    ): Boolean {
        val number = event.number ?: return false
        return context.contacts.isAvailable && context.contacts.isContact(number)
    }
}

/**
 * "Contacts only": rejects hidden numbers and numbers not in the contacts. Does nothing when the contacts cannot be
 * read (fails open, so a revoked permission never blocks every call).
 */
data class NotInContactsRule(
    override val id: RuleId,
    override val priority: Int,
    override val action: Action,
    override val enabled: Boolean = true,
) : Rule<IncomingCall> {
    override fun matches(
        event: IncomingCall,
        context: RuleContext,
    ): Boolean {
        val number = event.number
        return when {
            !context.contacts.isAvailable -> false
            number == null -> true
            else -> !context.contacts.isContact(number)
        }
    }
}
