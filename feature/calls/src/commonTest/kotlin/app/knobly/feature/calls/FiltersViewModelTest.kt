package app.knobly.feature.calls

import app.knobly.core.datastore.InMemoryDocumentStore
import app.knobly.domain.rules.call.CallFilterSettings
import app.knobly.domain.rules.call.CallFilterSettingsRepository
import app.knobly.domain.rules.call.CustomRule
import app.knobly.domain.rules.call.CustomRuleType
import app.knobly.platform.fake.FakeContactsAccess
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FiltersViewModelTest {
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
    fun `contacts only asks for the permission and turns on when granted`() =
        runTest {
            val contacts = FakeContactsAccess(granted = false, grantOnRequest = true)
            val viewModel = FiltersViewModel(repository, contacts)

            viewModel.setContactsOnly(true)

            assertTrue(repository.settings.first().contactsOnly)
            assertEquals(1, viewModel.state.value.activeFilterCount)
            assertFalse(viewModel.state.value.showContactsPermissionMessage)
        }

    @Test
    fun `contacts only stays off and explains when the permission is denied`() =
        runTest {
            val contacts = FakeContactsAccess(granted = false, grantOnRequest = false)
            val viewModel = FiltersViewModel(repository, contacts)

            viewModel.setContactsOnly(true)

            assertFalse(repository.settings.first().contactsOnly)
            assertTrue(viewModel.state.value.showContactsPermissionMessage)

            viewModel.openContactsSettings()
            assertEquals(1, contacts.settingsOpened)
        }

    @Test
    fun `revoked permission makes contacts only inactive and visible as a problem`() =
        runTest {
            store.set(CallFilterSettings(contactsOnly = true))
            val viewModel = FiltersViewModel(repository, FakeContactsAccess(granted = false))

            assertEquals(0, viewModel.state.value.activeFilterCount)
            assertTrue(viewModel.state.value.showContactsPermissionMessage)
        }

    @Test
    fun `custom rules group and per-rule toggles`() =
        runTest {
            store.set(CallFilterSettings(customRules = listOf(CustomRule("r1", CustomRuleType.PREFIX, "0303"))))
            val viewModel = FiltersViewModel(repository, FakeContactsAccess())

            viewModel.setCustomRulesEnabled(true)
            assertEquals(1, viewModel.state.value.activeFilterCount)

            viewModel.setRuleEnabled("r1", false)
            assertEquals(0, viewModel.state.value.activeFilterCount)
            assertFalse(viewModel.state.value.showContactsNotProtectedNote)
        }

    @Test
    fun `custom rules without the contacts permission warn that contacts are not protected`() =
        runTest {
            store.set(CallFilterSettings(customRules = listOf(CustomRule("r1", CustomRuleType.PREFIX, "0303"))))
            val viewModel = FiltersViewModel(repository, FakeContactsAccess(granted = false, grantOnRequest = false))

            viewModel.setCustomRulesEnabled(true)

            assertTrue(repository.settings.first().customRulesEnabled)
            assertTrue(viewModel.state.value.showContactsNotProtectedNote)
        }
}
