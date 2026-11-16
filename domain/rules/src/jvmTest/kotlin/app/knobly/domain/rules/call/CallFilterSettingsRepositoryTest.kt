package app.knobly.domain.rules.call

import app.knobly.core.datastore.InMemoryDocumentStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CallFilterSettingsRepositoryTest {
    @Test
    fun `settings saved before applyToContacts existed still load, with contacts protected`() {
        // call_filters.json as written by the first releases (no applyToContacts field).
        val old =
            """{"contactsOnly":false,"customRulesEnabled":true,"customRules":""" +
                """[{"id":"r1","type":"prefix","value":"0303","label":"Telemarketing","enabled":true}]}"""
        val settings = Json { ignoreUnknownKeys = true }.decodeFromString(CallFilterSettings.serializer(), old)

        assertEquals(CustomRule("r1", CustomRuleType.PREFIX, "0303", "Telemarketing"), settings.customRules.single())
        assertEquals(false, settings.customRules.single().applyToContacts)
    }

    @Test
    fun `toggles, saving, editing and deleting rules`() =
        runTest {
            val repository = CallFilterSettingsRepository(InMemoryDocumentStore(CallFilterSettings()))
            val rule = CustomRule("r1", CustomRuleType.PREFIX, "0303", "Telemarketing")

            repository.setContactsOnly(true)
            repository.setCustomRulesEnabled(true)
            repository.saveRule(rule)
            repository.saveRule(CustomRule("r2", CustomRuleType.HIDDEN_NUMBER))
            repository.saveRule(rule.copy(value = "0304"))
            repository.setRuleEnabled("r2", false)

            val settings = repository.settings.first()
            assertEquals(true, settings.contactsOnly)
            assertEquals(true, settings.customRulesEnabled)
            assertEquals(listOf("r1", "r2"), settings.customRules.map { it.id })
            assertEquals("0304", repository.rule("r1")?.value)
            assertEquals(false, repository.rule("r2")?.enabled)

            repository.deleteRule("r1")
            assertNull(repository.rule("r1"))
        }
}
