package app.knobly.app.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.knobly.app.shared.App
import app.knobly.app.shared.knoblyModules
import app.knobly.core.datastore.createJsonDocumentStore
import app.knobly.core.phone.LibPhoneNumberNormalizer
import app.knobly.core.phone.PhoneNumberNormalizer
import app.knobly.domain.port.ContactsAccess
import app.knobly.domain.port.ScreeningRoleController
import app.knobly.domain.rules.call.CallFilterSettings
import app.knobly.platform.fake.FakeContactsAccess
import app.knobly.platform.fake.FakeScreeningRoleController
import org.koin.core.context.startKoin
import org.koin.dsl.bind
import org.koin.dsl.module
import java.io.File

/** Width of the "phone" area, close to a typical Android phone in dp. */
private val PhoneWidth = 412.dp

fun main() {
    startKoin {
        modules(
            knoblyModules(
                module {
                    single { FakeScreeningRoleController() } bind ScreeningRoleController::class
                    single { FakeContactsAccess() } bind ContactsAccess::class
                    single<PhoneNumberNormalizer> { LibPhoneNumberNormalizer() }
                    // Persisted between harness runs, outside the project.
                    single {
                        createJsonDocumentStore(
                            path =
                                File(
                                    System.getProperty("java.io.tmpdir"),
                                    "knobly-desktop/call_filters.json",
                                ).absolutePath,
                            serializer = CallFilterSettings.serializer(),
                            default = CallFilterSettings(),
                            scope = get(),
                        )
                    }
                },
            ),
        )
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Knobly - desktop harness",
            state = rememberWindowState(width = 1120.dp, height = 880.dp),
        ) {
            Row {
                Box(Modifier.width(PhoneWidth).fillMaxHeight()) { App() }
                VerticalDivider()
                DevPanel(Modifier.fillMaxHeight())
            }
        }
    }
}
