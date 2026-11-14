package app.knobly.domain.rules.call

import app.knobly.core.datastore.InMemoryDocumentStore
import app.knobly.core.model.Action
import app.knobly.core.model.Decision
import app.knobly.core.model.IncomingCall
import app.knobly.core.phone.LibPhoneNumberNormalizer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * End-to-end behavior of the call filters (docs/specs/call-filters.md): settings -> rules -> decision, with the real
 * Brazilian number normalization.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CallFiltersTest {
    private val normalizer = LibPhoneNumberNormalizer()
    private val now = Instant.fromEpochSeconds(1_800_000_000)
    private val contact = "+5511912345678"
    private val stranger = "(21) 99876-5432"

    private fun rule(
        type: CustomRuleType,
        value: String = "",
        enabled: Boolean = true,
        id: String = type.name + value,
    ) = CustomRule(id = id, type = type, value = value, enabled = enabled)

    private suspend fun TestScope.screen(
        settings: CallFilterSettings,
        caller: String?,
        contacts: TestContacts = TestContacts(setOf(contact)),
    ): Decision {
        val provider = CallRulesProvider(InMemoryDocumentStore(settings).data, normalizer, backgroundScope)
        val screener = CallScreener(provider, contacts, backgroundScope, UnconfinedTestDispatcher(testScheduler))
        return screener.screen(IncomingCall(normalizer.normalize(caller), now))
    }

    @Test
    fun `everything off lets every call ring`() =
        runTest {
            assertEquals(Decision.Default, screen(CallFilterSettings(), "0303 123 4567"))
            assertEquals(Decision.Default, screen(CallFilterSettings(), null))
        }

    @Test
    fun `contacts only rejects strangers and hidden numbers but not contacts`() =
        runTest {
            val settings = CallFilterSettings(contactsOnly = true)
            assertEquals(Decision(Action.REJECT, CallFilterRuleIds.CONTACTS_ONLY), screen(settings, stranger))
            assertEquals(Decision(Action.REJECT, CallFilterRuleIds.CONTACTS_ONLY), screen(settings, null))
            assertEquals(Action.ALLOW, screen(settings, "(11) 91234-5678").action)
        }

    @Test
    fun `contacts only does nothing when the contacts permission is missing`() =
        runTest {
            val settings = CallFilterSettings(contactsOnly = true)
            assertEquals(Decision.Default, screen(settings, stranger, TestContacts(setOf(contact), granted = false)))
        }

    @Test
    fun `custom rules reject matching numbers and contacts always pass`() =
        runTest {
            val settings =
                CallFilterSettings(customRulesEnabled = true, customRules = listOf(rule(CustomRuleType.PREFIX, "011")))
            assertEquals(Action.REJECT, screen(settings, "011 3456-7890").action)
            // The contact also starts with 011, but contacts always pass.
            assertEquals(Decision(Action.ALLOW, CallFilterRuleIds.CONTACTS_ALLOW), screen(settings, "011 91234-5678"))
            assertEquals(Decision.Default, screen(settings, stranger))
        }

    @Test
    fun `custom rules group switch and per-rule switches`() =
        runTest {
            val telemarketing = rule(CustomRuleType.PREFIX, "0303")
            val groupOff = CallFilterSettings(customRulesEnabled = false, customRules = listOf(telemarketing))
            assertEquals(Decision.Default, screen(groupOff, "0303 123 4567"))

            val ruleOff =
                CallFilterSettings(customRulesEnabled = true, customRules = listOf(telemarketing.copy(enabled = false)))
            assertEquals(Decision.Default, screen(ruleOff, "0303 123 4567"))
        }

    @Test
    fun `filters add up`() =
        runTest {
            val settings =
                CallFilterSettings(
                    contactsOnly = true,
                    customRulesEnabled = true,
                    customRules = listOf(rule(CustomRuleType.PREFIX, "0303")),
                )
            assertEquals(Action.REJECT, screen(settings, "0303 123 4567").action)
            assertEquals(Action.REJECT, screen(settings, stranger).action)
            assertEquals(Action.ALLOW, screen(settings, contact).action)
        }

    @Test
    fun `each custom rule type`() =
        runTest {
            fun settings(vararg rules: CustomRule) =
                CallFilterSettings(customRulesEnabled = true, customRules = rules.toList())

            val prefix = settings(rule(CustomRuleType.PREFIX, "0303"))
            assertEquals(Action.REJECT, screen(prefix, "0303 123 4567").action)
            assertEquals(Action.REJECT, screen(prefix, "+55 303 123 4567").action)
            assertEquals(Action.ALLOW, screen(prefix, "0304 765 4321").action)

            val exact = settings(rule(CustomRuleType.EXACT_NUMBER, "(11) 4002-8922"))
            assertEquals(Action.REJECT, screen(exact, "+55 11 4002-8922").action)
            assertEquals(Action.ALLOW, screen(exact, "(11) 4002-8923").action)

            val hidden = settings(rule(CustomRuleType.HIDDEN_NUMBER))
            assertEquals(Action.REJECT, screen(hidden, null).action)
            assertEquals(Action.ALLOW, screen(hidden, stranger).action)

            val regex = settings(rule(CustomRuleType.REGEX, "^\\+1"))
            assertEquals(Action.REJECT, screen(regex, "+1 650 253 0000").action)
            assertEquals(Action.ALLOW, screen(regex, "+55 11 3456-7890").action)
        }

    @Test
    fun `invalid custom rules are ignored instead of breaking the screening`() =
        runTest {
            val settings =
                CallFilterSettings(customRulesEnabled = true, customRules = listOf(rule(CustomRuleType.REGEX, "([")))
            assertEquals(Decision.Default, screen(settings, "0303 123 4567"))
        }

    @Test
    fun `contacts are looked up at most once per call`() =
        runTest {
            val contacts = TestContacts(setOf(contact))
            screen(CallFilterSettings(contactsOnly = true), stranger, contacts)
            assertEquals(1, contacts.lookups)
        }

    @Test
    fun `active filter count`() {
        val rules = listOf(rule(CustomRuleType.PREFIX, "0303"), rule(CustomRuleType.HIDDEN_NUMBER, enabled = false))
        assertEquals(0, CallFilterSettings(customRules = rules).activeFilterCount)
        assertEquals(1, CallFilterSettings(customRulesEnabled = true, customRules = rules).activeFilterCount)
        assertEquals(
            2,
            CallFilterSettings(contactsOnly = true, customRulesEnabled = true, customRules = rules).activeFilterCount,
        )
    }
}
