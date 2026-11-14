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

    private fun TestScope.screener(
        settings: CallFilterSettings,
        contacts: TestContacts = TestContacts(listOf(contact)),
    ): CallScreener {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val provider = CallRulesProvider(InMemoryDocumentStore(settings).data, normalizer, backgroundScope)
        val directory = ContactDirectory(contacts, normalizer, backgroundScope, dispatcher)
        return CallScreener(provider, directory, backgroundScope, dispatcher)
    }

    private suspend fun CallScreener.screen(caller: String?): Decision =
        screen(IncomingCall(normalizer.normalize(caller), now))

    private suspend fun TestScope.screen(
        settings: CallFilterSettings,
        caller: String?,
        contacts: TestContacts = TestContacts(listOf(contact)),
    ): Decision = screener(settings, contacts).screen(caller)

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
            assertEquals(Decision.Default, screen(settings, stranger, TestContacts(listOf(contact), granted = false)))
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
    fun `contacts are read once and again only when they change`() =
        runTest {
            val contacts = TestContacts(listOf(contact))
            val screener = screener(CallFilterSettings(contactsOnly = true), contacts)

            screener.screen(stranger)
            screener.screen("(11) 91234-5678")
            assertEquals(1, contacts.reads)

            // No notification needed: the next call sees that the contacts changed.
            contacts.numbers = listOf(contact, stranger)
            assertEquals(Action.ALLOW, screener.screen(stranger).action)
            assertEquals(2, contacts.reads)
        }

    @Test
    fun `contacts saved without the 9th digit, the DDD or the country code still pass`() =
        runTest {
            val settings = CallFilterSettings(contactsOnly = true)
            val oldFormat = TestContacts(listOf("(35) 8123-4567", "8765-4321"))

            assertEquals(Action.ALLOW, screen(settings, "+55 35 98123-4567", oldFormat).action)
            assertEquals(Action.ALLOW, screen(settings, "(11) 98765-4321", oldFormat).action)
            assertEquals(Action.ALLOW, screen(settings, "035 98765-4321", oldFormat).action)
            // Same ending, other DDD: not the saved number.
            assertEquals(Action.REJECT, screen(settings, "(11) 98123-4567", oldFormat).action)
        }

    @Test
    fun `contacts in the old format are also protected from custom rules`() =
        runTest {
            val settings =
                CallFilterSettings(customRulesEnabled = true, customRules = listOf(rule(CustomRuleType.PREFIX, "35")))
            val oldFormat = TestContacts(listOf("(35) 8123-4567"))

            assertEquals(
                Decision(Action.ALLOW, CallFilterRuleIds.CONTACTS_ALLOW),
                screen(settings, "35 98123-4567", oldFormat),
            )
            assertEquals(Action.REJECT, screen(settings, "35 98123-0000", oldFormat).action)
        }

    @Test
    fun `an exact number rule matches with or without the 9th digit and the DDD`() =
        runTest {
            val old =
                CallFilterSettings(
                    customRulesEnabled = true,
                    customRules = listOf(rule(CustomRuleType.EXACT_NUMBER, "(35) 8123-4567")),
                )
            assertEquals(Action.REJECT, screen(old, "+55 35 98123-4567").action)

            val noDdd =
                CallFilterSettings(
                    customRulesEnabled = true,
                    customRules = listOf(rule(CustomRuleType.EXACT_NUMBER, "98123-4567")),
                )
            assertEquals(Action.REJECT, screen(noDdd, "(35) 8123-4567").action)
            assertEquals(Action.ALLOW, screen(noDdd, "(35) 98123-4568").action)
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
