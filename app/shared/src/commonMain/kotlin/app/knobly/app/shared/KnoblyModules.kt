package app.knobly.app.shared

import app.knobly.core.model.IncomingCall
import app.knobly.domain.rules.RuleEngine
import app.knobly.domain.rules.call.SpikeRules
import app.knobly.feature.onboarding.CallFilteringViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Platform-independent dependency graph. */
val sharedModule: Module =
    module {
        // Rules are kept in memory: the Android CallScreeningService must answer within a few seconds.
        // Spike: hard-coded rules until rules are persisted (:core:database) and edited in :feature:calls.
        single { RuleEngine<IncomingCall> { SpikeRules.calls } }
        viewModelOf(::CallFilteringViewModel)
    }

/**
 * Every Koin module of the app. [platformModule] provides the platform ports (real adapters on Android/iOS,
 * fakes on the desktop harness and in tests).
 */
fun knoblyModules(platformModule: Module): List<Module> = listOf(sharedModule, platformModule)
