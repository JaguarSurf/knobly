package app.knobly.app.shared

import androidx.compose.ui.window.ComposeUIViewController
import app.knobly.domain.port.ScreeningRoleController
import app.knobly.platform.ios.calldirectory.IosScreeningRoleController
import org.koin.core.context.startKoin
import org.koin.dsl.module
import platform.UIKit.UIViewController

/** Called once from Swift (`iOSApp.init`) before any UI is shown. */
fun initKoin() {
    startKoin {
        modules(
            knoblyModules(
                module { single<ScreeningRoleController> { IosScreeningRoleController() } },
            ),
        )
    }
}

/** Hosts the shared Compose UI in the SwiftUI app (`ContentView.swift`). */
@Suppress("FunctionName", "ktlint:standard:function-naming")
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
