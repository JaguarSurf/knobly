package app.knobly.domain.rules.call

import app.knobly.core.model.RuleId

/** Rule ids and priorities of the call filters. Lower priorities are evaluated first. */
object CallFilterRuleIds {
    val CONTACTS_ALLOW = RuleId("contacts-allow")
    val CONTACTS_ONLY = RuleId("contacts-only")

    fun custom(rule: CustomRule) = RuleId("custom:${rule.id}")

    /** Custom rules that also apply to contacts ([CustomRule.applyToContacts]) come before the contacts. */
    internal const val CUSTOM_OVER_CONTACTS_PRIORITY_START = -100_000
    internal const val CONTACTS_ALLOW_PRIORITY = 0
    internal const val CUSTOM_PRIORITY_START = 100
    internal const val CONTACTS_ONLY_PRIORITY = 10_000
}
