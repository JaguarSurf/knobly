package app.knobly.app.shared

import app.knobly.core.datastore.DocumentStoreFactory
import app.knobly.core.datastore.InMemoryDocumentStore
import app.knobly.core.datastore.InMemoryDocumentStoreFactory
import app.knobly.core.phone.LibPhoneNumberNormalizer
import app.knobly.core.phone.PhoneNumberNormalizer
import app.knobly.domain.history.CallHistoryRepository
import app.knobly.domain.history.CallRecord
import app.knobly.domain.history.InMemoryCallHistoryRepository
import app.knobly.domain.port.CallLogAccess
import app.knobly.domain.port.ContactsAccess
import app.knobly.domain.port.ScreeningRoleController
import app.knobly.domain.rules.call.CallFilterSettings
import app.knobly.domain.settings.AppSettings
import app.knobly.domain.settings.ThemeMode
import app.knobly.platform.fake.FakeCallLogAccess
import app.knobly.platform.fake.FakeContactsAccess
import app.knobly.platform.fake.FakeScreeningRoleController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.bind
import org.koin.dsl.module
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.Instant

/** "Now" in UI tests: Saturday 2026-11-14, 14:00 in São Paulo. */
internal val TEST_NOW: Instant = Instant.parse("2026-11-14T17:00:00Z")
internal val TEST_TIME_ZONE: TimeZone = TimeZone.of("America/Sao_Paulo")

private object FixedClock : Clock {
    override fun now() = TEST_NOW
}

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
    history: List<CallRecord> = emptyList(),
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    callLogGranted: Boolean = true,
): TestEnv {
    Dispatchers.setMain(UnconfinedTestDispatcher())
    Locale.setDefault(Locale.forLanguageTag("pt-BR"))
    val env =
        TestEnv(
            role = FakeScreeningRoleController(roleHeld = roleHeld),
            contacts = FakeContactsAccess(granted = contactsGranted),
            settings = InMemoryDocumentStore(settings),
            history = InMemoryCallHistoryRepository(history),
            appSettings = InMemoryDocumentStore(AppSettings(themeMode)),
            callLog = FakeCallLogAccess(granted = callLogGranted),
        )
    startKoin {
        modules(
            knoblyModules(
                module {
                    single { env.role } bind ScreeningRoleController::class
                    single { env.contacts } bind ContactsAccess::class
                    single { env.callLog } bind CallLogAccess::class
                    single<PhoneNumberNormalizer> { LibPhoneNumberNormalizer() }
                    single<DocumentStoreFactory> {
                        InMemoryDocumentStoreFactory(
                            mapOf(
                                CallFilterSettings.DOCUMENT_NAME to env.settings,
                                AppSettings.DOCUMENT_NAME to env.appSettings,
                            ),
                        )
                    }
                    // Overrides of the shared graph: no database, and a fixed "now" for stable screenshots.
                    single<CallHistoryRepository> { env.history }
                    single<Clock> { FixedClock }
                    factory { TEST_TIME_ZONE }
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
