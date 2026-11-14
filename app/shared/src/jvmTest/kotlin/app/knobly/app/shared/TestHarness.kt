package app.knobly.app.shared

import app.knobly.core.datastore.DocumentStore
import app.knobly.core.datastore.InMemoryDocumentStore
import app.knobly.core.phone.LibPhoneNumberNormalizer
import app.knobly.core.phone.PhoneNumberNormalizer
import app.knobly.domain.port.ContactsAccess
import app.knobly.domain.port.ScreeningRoleController
import app.knobly.domain.rules.call.CallFilterSettings
import app.knobly.platform.fake.FakeContactsAccess
import app.knobly.platform.fake.FakeScreeningRoleController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.bind
import org.koin.dsl.module
import java.util.Locale

/**
 * Starts the real shared graph with fake platform adapters and in-memory settings.
 *
 * Also installs a test `Dispatchers.Main` (on the desktop JVM there is no main dispatcher unless
 * kotlinx-coroutines-swing is present, and AndroidX Lifecycle/Navigation check it to enforce "main thread" access)
 * and runs in Brazilian Portuguese, the app's main language.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun startTestKoin(
    roleHeld: Boolean,
    contactsGranted: Boolean = true,
    settings: CallFilterSettings = CallFilterSettings(),
): TestEnv {
    Dispatchers.setMain(UnconfinedTestDispatcher())
    Locale.setDefault(Locale.forLanguageTag("pt-BR"))
    val env =
        TestEnv(
            role = FakeScreeningRoleController(roleHeld = roleHeld),
            contacts = FakeContactsAccess(granted = contactsGranted),
            settings = InMemoryDocumentStore(settings),
        )
    startKoin {
        modules(
            knoblyModules(
                module {
                    single { env.role } bind ScreeningRoleController::class
                    single { env.contacts } bind ContactsAccess::class
                    single<PhoneNumberNormalizer> { LibPhoneNumberNormalizer() }
                    single<DocumentStore<CallFilterSettings>> { env.settings }
                },
            ),
        )
    }
    return env
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun stopTestKoin() {
    stopKoin()
    Dispatchers.resetMain()
}
