package app.knobly.app.shared

import app.knobly.core.database.KnoblyDatabase
import app.knobly.core.datastore.DocumentStoreFactory
import app.knobly.domain.history.CallHistoryRecorder
import app.knobly.domain.history.CallHistoryRepository
import app.knobly.domain.history.CallLogSync
import app.knobly.domain.history.RoomCallHistoryRepository
import app.knobly.domain.rules.call.CallFilterSettings
import app.knobly.domain.rules.call.CallFilterSettingsRepository
import app.knobly.domain.rules.call.CallRulesProvider
import app.knobly.domain.rules.call.CallScreener
import app.knobly.domain.rules.call.ContactDirectory
import app.knobly.domain.rules.call.SCREENING_DISPATCHER
import app.knobly.domain.settings.AppSettings
import app.knobly.domain.settings.AppSettingsRepository
import app.knobly.domain.stats.CallStatistics
import app.knobly.feature.calls.FiltersViewModel
import app.knobly.feature.calls.RuleEditorViewModel
import app.knobly.feature.history.HistoryViewModel
import app.knobly.feature.home.HomeViewModel
import app.knobly.feature.home.ThemeViewModel
import app.knobly.feature.onboarding.CallFilteringViewModel
import app.knobly.feature.stats.StatsViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.datetime.TimeZone
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.time.Clock

/**
 * Platform-independent dependency graph. Each platform module provides the ports (ScreeningRoleController,
 * ContactsAccess, CallLogAccess), the PhoneNumberNormalizer, a DocumentStoreFactory and the KnoblyDatabase (their
 * file paths are platform specific).
 */
val sharedModule: Module =
    module {
        // Lives as long as the process: keeps the in-memory rules up to date for the call screening service.
        single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
        single<Clock> { Clock.System }
        // A factory: the user may change the time zone while the app is running.
        factory { TimeZone.currentSystemDefault() }
        single {
            CallFilterSettingsRepository(
                get<DocumentStoreFactory>().create(
                    CallFilterSettings.DOCUMENT_NAME,
                    CallFilterSettings.serializer(),
                    CallFilterSettings(),
                ),
            )
        }
        // Created at start: the saved theme is read before the first frame.
        single(createdAtStart = true) {
            AppSettingsRepository(
                get<DocumentStoreFactory>().create(AppSettings.DOCUMENT_NAME, AppSettings.serializer(), AppSettings()),
                get(),
            )
        }
        single<CallHistoryRepository> { RoomCallHistoryRepository(get<KnoblyDatabase>().callEvents()) }
        single { CallHistoryRecorder(get(), get<CallFilterSettingsRepository>().settings, get()) }
        single {
            CallLogSync(
                history = get(),
                callLog = get(),
                normalizer = get(),
                clock = get(),
                dispatcher = getOrNull<CoroutineDispatcher>(named(SCREENING_DISPATCHER)) ?: Dispatchers.Default,
            )
        }
        single { CallStatistics(get(), get()) { get<TimeZone>() } }
        single { CallRulesProvider(get<CallFilterSettingsRepository>().settings, get(), get()) }
        single {
            ContactDirectory(
                contacts = get(),
                normalizer = get(),
                scope = get(),
                dispatcher = getOrNull<CoroutineDispatcher>(named(SCREENING_DISPATCHER)) ?: Dispatchers.Default,
            )
        }
        single {
            CallScreener(
                rules = get(),
                contacts = get(),
                scope = get(),
                dispatcher = getOrNull<CoroutineDispatcher>(named(SCREENING_DISPATCHER)) ?: Dispatchers.Default,
            )
        }
        viewModelOf(::CallFilteringViewModel)
        viewModelOf(::HomeViewModel)
        viewModelOf(::FiltersViewModel)
        viewModel { (ruleId: String?) -> RuleEditorViewModel(ruleId, get(), get()) }
        viewModelOf(::HistoryViewModel)
        viewModelOf(::StatsViewModel)
        viewModelOf(::ThemeViewModel)
    }

/** Every Koin module of the app: the shared graph plus the [platformModule] (real adapters, or fakes). */
fun knoblyModules(platformModule: Module): List<Module> = listOf(sharedModule, platformModule)
