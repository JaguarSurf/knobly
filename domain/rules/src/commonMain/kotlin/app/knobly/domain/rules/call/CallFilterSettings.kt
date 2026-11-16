package app.knobly.domain.rules.call

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * What the user configured in the Filters tab (docs/specs/call-filters.md). Persisted as one JSON document; every
 * filter is off by default.
 */
@Serializable
data class CallFilterSettings(
    val contactsOnly: Boolean = false,
    val customRulesEnabled: Boolean = false,
    val customRules: List<CustomRule> = emptyList(),
) {
    /** Filters that currently reject calls: "contacts only" plus each enabled custom rule while the group is on. */
    val activeFilterCount: Int
        get() = (if (contactsOnly) 1 else 0) + (if (customRulesEnabled) customRules.count { it.enabled } else 0)

    companion object {
        /** Stored as `call_filters.json`: the name must never change, or users lose their filters. */
        const val DOCUMENT_NAME = "call_filters"
    }
}

/**
 * A user-defined rule: `type + value`, plus a label and its own on/off switch.
 *
 * Contacts always pass custom rules, unless [applyToContacts] is on: then the rule also rejects numbers saved in the
 * contacts. It has no effect on [CustomRuleType.HIDDEN_NUMBER] (a hidden number is never a contact).
 */
@Serializable
data class CustomRule(
    val id: String,
    val type: CustomRuleType,
    val value: String = "",
    val label: String = "",
    val enabled: Boolean = true,
    // Added after the first release: the default keeps rules saved before it working as they did.
    val applyToContacts: Boolean = false,
) {
    /** Whether this rule is evaluated before "contacts always pass" (see [applyToContacts]). */
    val appliesToContacts: Boolean
        get() = applyToContacts && type != CustomRuleType.HIDDEN_NUMBER
}

@Serializable
enum class CustomRuleType {
    /** Number starts with digits such as `0303`, `0304`, `+1`; the trunk prefix `0` is optional. */
    @SerialName("prefix")
    PREFIX,

    /** One specific number. */
    @SerialName("exact")
    EXACT_NUMBER,

    /** Calls without caller id. */
    @SerialName("hidden")
    HIDDEN_NUMBER,

    /** Regular expression over the number's digits (advanced, Android only). */
    @SerialName("regex")
    REGEX,
}
