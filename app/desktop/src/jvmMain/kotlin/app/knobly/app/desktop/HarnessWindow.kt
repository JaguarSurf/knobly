package app.knobly.app.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.unit.DpSize
import app.knobly.app.shared.App
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import javax.swing.JFrame
import kotlin.math.roundToInt

/** Width of the dev panel next to the simulated phone. */
private const val PANEL_WIDTH = 720

/**
 * The harness window: the app and the dev panel side by side, each in its own Compose scene.
 *
 * The app's scene is exactly the simulated phone's screen, so its popups, dialogs and bottom sheets stay inside the
 * phone like on a real device (in a single scene for the whole window they would cover the dev panel too).
 * The window is packed around the phone (AWT units are dp on the desktop) and follows preset changes.
 */
fun showHarnessWindow() {
    val device = DeviceFrameState()
    val frame = JFrame("Knobly - desktop harness")
    val phone =
        ComposePanel().apply {
            preferredSize = device.size.toDimension()
            setContent { DeviceScreen(device) { App() } }
        }
    val tools =
        ComposePanel().apply {
            // Only the width counts: the window is as tall as the phone, and the panel scrolls.
            preferredSize = Dimension(PANEL_WIDTH, 0)
            setContent {
                DevPanel(device, Modifier.fillMaxSize())
                // Follow the simulated phone (preset, landscape) so it is shown whole.
                LaunchedEffect(device.size) {
                    phone.preferredSize = device.size.toDimension()
                    frame.fitToContent()
                }
            }
        }
    frame.apply {
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        contentPane.add(phone, BorderLayout.WEST)
        contentPane.add(tools, BorderLayout.CENTER)
        fitToContent()
        setLocationRelativeTo(null)
        isVisible = true
    }
}

/**
 * Sizes the window around its content, title bar and borders included, but never larger than the usable area of the
 * screen (without the taskbar); on a smaller screen the phone gets the height left.
 */
private fun JFrame.fitToContent() {
    pack()
    val screen = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
    setSize(width.coerceAtMost(screen.width), height.coerceAtMost(screen.height))
}

private fun DpSize.toDimension() = Dimension(width.value.roundToInt(), height.value.roundToInt())
