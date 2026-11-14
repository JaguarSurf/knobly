package app.knobly.domain.rules.call

import app.knobly.core.model.Action
import app.knobly.core.model.IncomingCall
import app.knobly.core.phone.PhoneNumberNormalizer
import app.knobly.domain.rules.Rule

/**
 * Turns the user's [CallFilterSettings] into engine rules.
 *
 * Filters add up (any enabled filter rejects), with one exception: contacts always pass, so an allow rule for
 * contacts comes first whenever any filter is on. With every filter off the list is empty and every call rings.
 */
fun buildCallRules(
    settings: CallFilterSettings,
    normalizer: PhoneNumberNormalizer,
): List<Rule<IncomingCall>> {
    val custom =
        if (settings.customRulesEnabled) {
            settings.customRules
                .filter { it.enabled }
                .mapIndexedNotNull { index, rule ->
                    buildCustomRule(
                        rule,
                        CallFilterRuleIds.CUSTOM_PRIORITY_START + index,
                        normalizer,
                    )
                }
        } else {
            emptyList()
        }
    val contactsOnly =
        if (settings.contactsOnly) {
            listOf(
                NotInContactsRule(
                    CallFilterRuleIds.CONTACTS_ONLY,
                    CallFilterRuleIds.CONTACTS_ONLY_PRIORITY,
                    Action.REJECT,
                ),
            )
        } else {
            emptyList()
        }
    if (custom.isEmpty() && contactsOnly.isEmpty()) return emptyList()
    return listOf(ContactAllowRule(CallFilterRuleIds.CONTACTS_ALLOW, CallFilterRuleIds.CONTACTS_ALLOW_PRIORITY)) +
        custom +
        contactsOnly
}

/** One custom rule as an engine rule (action: reject), or `null` if its value is not valid. */
fun buildCustomRule(
    rule: CustomRule,
    priority: Int,
    normalizer: PhoneNumberNormalizer,
): Rule<IncomingCall>? {
    if (CustomRuleValidator.validate(rule.type, rule.value, normalizer) != null) return null
    val id = CallFilterRuleIds.custom(rule)
    return when (rule.type) {
        CustomRuleType.PREFIX -> {
            PrefixRule(
                id,
                priority,
                Action.REJECT,
                listOf(CustomRuleValidator.normalizedPrefix(rule.value)),
            )
        }

        CustomRuleType.EXACT_NUMBER -> {
            normalizer.normalize(rule.value)?.let {
                ExactNumberRule(id, priority, Action.REJECT, it)
            }
        }

        CustomRuleType.HIDDEN_NUMBER -> {
            HiddenNumberRule(id, priority, Action.REJECT)
        }

        CustomRuleType.REGEX -> {
            RegexRule(id, priority, Action.REJECT, Regex(rule.value.trim()))
        }
    }
}
