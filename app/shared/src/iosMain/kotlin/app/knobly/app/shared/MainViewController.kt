package app.knobly.app.shared

import androidx.compose.ui.window.ComposeUIViewController
import app.knobly.core.datastore.createJsonDocumentStore
import app.knobly.core.phone.DigitsOnlyPhoneNumberNormalizer
import app.knobly.core.phone.PhoneNumberNormalizer
import app.knobly.domain.port.ContactsAccess
import app.knobly.domain.port.ScreeningRoleController
import app.knobly.domain.rules.call.CallFilterSettings
import app.knobly.platform.ios.calldirectory.IosContactsAccess
import app.knobly.platform.ios.calldirectory.IosScreeningRoleController
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.context.startKoin
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIViewController

/** Called once from Swift (`iOSApp.init`) before any UI is shown. */
fun initKoin() {
    startKoin {
        modules(
            knoblyModules(
                module {
                    single<ScreeningRoleController> { IosScreeningRoleController() }
                    single<ContactsAccess> { IosContactsAccess() }
                    // TODO(ios): PhoneNumberKit from Swift (docs/architecture.md, Platform ports).
                    single<PhoneNumberNormalizer> { DigitsOnlyPhoneNumberNormalizer() }
                    single {
                        createJsonDocumentStore(
                            path = documentsDirectory() + "/call_filters.json",
                            serializer = CallFilterSettings.serializer(),
                            default = CallFilterSettings(),
                            scope = get(),
                        )
                    }
                },
            ),
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun documentsDirectory(): String =
    requireNotNull(
        NSFileManager.defaultManager
            .URLForDirectory(NSDocumentDirectory, NSUserDomainMask, null, true, null)
            ?.path,
    ) { "No documents directory" }

/** Hosts the shared Compose UI in the SwiftUI app (`ContentView.swift`). */
@Suppress("FunctionName", "ktlint:standard:function-naming")
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
