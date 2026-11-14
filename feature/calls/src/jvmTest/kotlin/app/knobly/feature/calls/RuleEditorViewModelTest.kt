package app.knobly.feature.calls

import app.knobly.core.datastore.InMemoryDocumentStore
import app.knobly.core.phone.LibPhoneNumberNormalizer
import app.knobly.domain.rules.call.CallFilterSettings
import app.knobly.domain.rules.call.CallFilterSettingsRepository
import app.knobly.domain.rules.call.CustomRule
import app.knobly.domain.rules.call.CustomRuleError
import app.knobly.domain.rules.call.CustomRuleType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RuleEditorViewModelTest {
    private val normalizer = LibPhoneNumberNormalizer()
    private val store = InMemoryDocumentStore(CallFilterSettings())
    private val repository = CallFilterSettingsRepository(store)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `invalid values are not saved`() =
        runTest {
            val viewModel = RuleEditorViewModel(null, repository, normalizer)
            viewModel.setType(CustomRuleType.REGEX)
            viewModel.setValue("([")
            viewModel.save()

            assertEquals(CustomRuleError.INVALID_REGEX, viewModel.state.value.error)
            assertTrue(
                repository.settings
                    .first()
                    .customRules
                    .isEmpty(),
            )
        }

    @Test
    fun `a new rule is saved enabled and closes the editor`() =
        runTest {
            val viewModel = RuleEditorViewModel(null, repository, normalizer)
            viewModel.setValue("0303")
            viewModel.setLabel(" Telemarketing ")
            viewModel.save()

            val saved =
                repository.settings
                    .first()
                    .customRules
                    .single()
            assertEquals(CustomRuleType.PREFIX, saved.type)
            assertEquals("0303", saved.value)
            assertEquals("Telemarketing", saved.label)
            assertTrue(saved.enabled)
            assertTrue(viewModel.state.value.done)
        }

    @Test
    fun `editing keeps the id and the enabled state, and delete removes the rule`() =
        runTest {
            store.set(
                CallFilterSettings(
                    customRules = listOf(CustomRule("r1", CustomRuleType.PREFIX, "0303", enabled = false)),
                ),
            )
            val viewModel = RuleEditorViewModel("r1", repository, normalizer)
            assertEquals("0303", viewModel.state.value.value)

            viewModel.setValue("0304")
            viewModel.save()
            assertEquals(CustomRule("r1", CustomRuleType.PREFIX, "0304", enabled = false), repository.rule("r1"))

            RuleEditorViewModel("r1", repository, normalizer).delete()
            assertNull(repository.rule("r1"))
        }

    @Test
    fun `trying a number uses the real rule`() =
        runTest {
            val viewModel = RuleEditorViewModel(null, repository, normalizer)
            viewModel.setValue("0303")
            viewModel.setTestNumber("+55 303 123 4567")
            assertEquals(true, viewModel.state.value.testRejected)

            viewModel.setTestNumber("(11) 91234-5678")
            assertEquals(false, viewModel.state.value.testRejected)

            viewModel.setType(CustomRuleType.HIDDEN_NUMBER)
            assertNull(viewModel.state.value.testRejected)
        }
}
