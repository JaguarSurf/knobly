package app.knobly.domain.rules.call

import app.knobly.core.datastore.DocumentStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Reads and changes the call filter settings. Changes are persisted and reach the call screening immediately. */
class CallFilterSettingsRepository(
    private val store: DocumentStore<CallFilterSettings>,
) {
    val settings: Flow<CallFilterSettings> = store.data

    suspend fun setContactsOnly(enabled: Boolean) {
        store.update { it.copy(contactsOnly = enabled) }
    }

    suspend fun setCustomRulesEnabled(enabled: Boolean) {
        store.update { it.copy(customRulesEnabled = enabled) }
    }

    suspend fun setRuleEnabled(
        id: String,
        enabled: Boolean,
    ) {
        store.update { settings ->
            settings.copy(
                customRules =
                    settings.customRules.map {
                        if (it.id ==
                            id
                        ) {
                            it.copy(enabled = enabled)
                        } else {
                            it
                        }
                    },
            )
        }
    }

    /** Adds [rule], or replaces the rule with the same id (keeping its position). */
    suspend fun saveRule(rule: CustomRule) {
        store.update { settings ->
            val exists = settings.customRules.any { it.id == rule.id }
            settings.copy(
                customRules =
                    if (exists) {
                        settings.customRules.map {
                            if (it.id ==
                                rule.id
                            ) {
                                rule
                            } else {
                                it
                            }
                        }
                    } else {
                        settings.customRules + rule
                    },
            )
        }
    }

    suspend fun deleteRule(id: String) {
        store.update { settings -> settings.copy(customRules = settings.customRules.filterNot { it.id == id }) }
    }

    suspend fun rule(id: String): CustomRule? = settings.first().customRules.firstOrNull { it.id == id }
}
