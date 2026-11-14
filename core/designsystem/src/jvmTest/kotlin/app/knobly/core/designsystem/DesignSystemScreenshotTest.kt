package app.knobly.core.designsystem

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

/** `./gradlew :core:designsystem:recordRoborazziJvm` writes the PNGs to core/designsystem/build/outputs/roborazzi. */
@OptIn(ExperimentalTestApi::class)
class DesignSystemScreenshotTest {
    private fun catalog(dark: Boolean) =
        runDesktopComposeUiTest(width = 412, height = 892) {
            setContent { KnoblyTheme(darkTheme = dark) { DesignSystemCatalog() } }
            onRoot().captureRoboImage("build/outputs/roborazzi/catalog-${if (dark) "dark" else "light"}.png")
        }

    @Test
    fun catalogDark() = catalog(dark = true)

    @Test
    fun catalogLight() = catalog(dark = false)
}
