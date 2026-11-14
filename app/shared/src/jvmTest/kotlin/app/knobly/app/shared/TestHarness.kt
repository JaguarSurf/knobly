package app.knobly.app.shared

import app.knobly.domain.port.ScreeningRoleController
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
 * Starts the real shared graph with fake platform adapters; returns the fake role controller.
 *
 * Also installs a test `Dispatchers.Main`: on the desktop JVM there is no main dispatcher unless
 * kotlinx-coroutines-swing is present, and AndroidX Lifecycle/Navigation check it to enforce "main thread" access.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun startTestKoin(roleHeld: Boolean): FakeScreeningRoleController {
    Dispatchers.setMain(UnconfinedTestDispatcher())
    // UI tests and screenshots run in Brazilian Portuguese, the app's main language.
    Locale.setDefault(Locale.forLanguageTag("pt-BR"))
    val roleController = FakeScreeningRoleController(roleHeld = roleHeld)
    startKoin {
        modules(knoblyModules(module { single { roleController } bind ScreeningRoleController::class }))
    }
    return roleController
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun stopTestKoin() {
    stopKoin()
    Dispatchers.resetMain()
}
