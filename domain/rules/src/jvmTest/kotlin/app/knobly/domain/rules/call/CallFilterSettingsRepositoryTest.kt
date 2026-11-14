package app.knobly.domain.rules.call

import app.knobly.core.datastore.InMemoryDocumentStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CallFilterSettingsRepositoryTest {
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
