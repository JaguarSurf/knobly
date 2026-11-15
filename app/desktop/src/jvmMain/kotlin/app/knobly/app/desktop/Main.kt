package app.knobly.app.desktop

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.room.Room
import app.knobly.app.shared.App
import app.knobly.app.shared.knoblyModules
import app.knobly.core.database.KnoblyDatabase
import app.knobly.core.database.buildKnoblyDatabase
import app.knobly.core.datastore.DocumentStoreFactory
import app.knobly.core.datastore.JsonDocumentStoreFactory
import app.knobly.core.phone.LibPhoneNumberNormalizer
import app.knobly.core.phone.PhoneNumberNormalizer
import app.knobly.domain.port.CallLogAccess
import app.knobly.domain.port.ContactsAccess
import app.knobly.domain.port.ScreeningRoleController
import app.knobly.platform.fake.FakeCallLogAccess
import app.knobly.platform.fake.FakeContactsAccess
import app.knobly.platform.fake.FakeScreeningRoleController
import org.koin.core.context.startKoin
import org.koin.dsl.bind
import org.koin.dsl.module
import java.awt.GraphicsEnvironment
import java.awt.Insets
import java.io.File

/** Width of the dev panel next to the simulated phone. */
private val PanelWidth = 720.dp

/**
 * A window whose content area is exactly the simulated phone next to the dev panel, but never larger than the usable
 * area of the screen (without the taskbar); a phone taller than that scrolls inside the window.
 *
 * The window size includes its [decorations] (title bar and borders, from the window manager), so they are added.
 * AWT sizes are in logical units, the same as dp on the desktop.
 */
private fun windowSizeFor(
    phone: DpSize,
    decorations: Insets,
): DpSize {
    val screen = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
    return DpSize(
        (phone.width + PanelWidth + (decorations.left + decorations.right).dp).coerceAtMost(screen.width.dp),
        (phone.height + (decorations.top + decorations.bottom).dp).coerceAtMost(screen.height.dp),
    )
}

/** The harness's settings and database (delete it to start over). */
private val harnessDir = File(System.getProperty("java.io.tmpdir"), "knobly-desktop").apply { mkdirs() }

fun main() {
    startKoin {
        modules(
            knoblyModules(
                module {
                    single { FakeScreeningRoleController() } bind ScreeningRoleController::class
                    single { FakeContactsAccess() } bind ContactsAccess::class
                    single { FakeCallLogAccess() } bind CallLogAccess::class
                    single<PhoneNumberNormalizer> { LibPhoneNumberNormalizer() }
                    // Persisted between harness runs, outside the project.
                    single {
                        Room
                            .databaseBuilder<KnoblyDatabase>(
                                name = File(harnessDir, KnoblyDatabase.FILE_NAME).absolutePath,
                            ).buildKnoblyDatabase()
                    }
                    single<DocumentStoreFactory> { JsonDocumentStoreFactory(harnessDir.absolutePath, scope = get()) }
                },
            ),
        )
    }

    application {
        val device = remember { DeviceFrameState() }
        // Without decorations until the window exists; corrected below before it is shown.
        val windowState = rememberWindowState(size = windowSizeFor(device.size, Insets(0, 0, 0, 0)))
        Window(
            onCloseRequest = ::exitApplication,
            title = "Knobly - desktop harness",
            state = windowState,
        ) {
            // Follow the simulated phone (preset, landscape) so it is shown whole and the dev panel keeps its room.
            LaunchedEffect(device.size) { windowState.size = windowSizeFor(device.size, window.insets) }
            Row {
                DeviceFrame(device, Modifier.fillMaxHeight()) { App() }
                VerticalDivider()
                DevPanel(device, Modifier.fillMaxHeight())
            }
        }
    }
}
