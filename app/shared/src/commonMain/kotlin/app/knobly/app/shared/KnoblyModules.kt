package app.knobly.app.shared

import app.knobly.core.database.KnoblyDatabase
import app.knobly.domain.history.CallHistoryRecorder
import app.knobly.domain.history.CallHistoryRepository
import app.knobly.domain.history.RoomCallHistoryRepository
import app.knobly.domain.rules.call.CallFilterSettingsRepository
import app.knobly.domain.rules.call.CallRulesProvider
import app.knobly.domain.rules.call.CallScreener
import app.knobly.domain.rules.call.SCREENING_DISPATCHER
import app.knobly.domain.stats.CallStatistics
import app.knobly.feature.calls.FiltersViewModel
import app.knobly.feature.calls.RuleEditorViewModel
import app.knobly.feature.history.HistoryViewModel
import app.knobly.feature.home.HomeViewModel
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
 * ContactsAccess), the PhoneNumberNormalizer, the DocumentStore of the call filter settings and the KnoblyDatabase
 * (their file paths are platform specific).
 */
val sharedModule: Module =
    module {
        // Lives as long as the process: keeps the in-memory rules up to date for the call screening service.
        single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
        single<Clock> { Clock.System }
        // A factory: the user may change the time zone while the app is running.
        factory { TimeZone.currentSystemDefault() }
        single { CallFilterSettingsRepository(get()) }
        single<CallHistoryRepository> { RoomCallHistoryRepository(get<KnoblyDatabase>().callEvents()) }
        single { CallHistoryRecorder(get(), get<CallFilterSettingsRepository>().settings, get()) }
        single { CallStatistics(get(), get()) { get<TimeZone>() } }
        single { CallRulesProvider(get<CallFilterSettingsRepository>().settings, get(), get()) }
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
    }

/** Every Koin module of the app: the shared graph plus the [platformModule] (real adapters, or fakes). */
fun knoblyModules(platformModule: Module): List<Module> = listOf(sharedModule, platformModule)
