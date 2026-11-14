package app.knobly.app.shared

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.AfterTest
import kotlin.test.Test

/**
 * Roborazzi screenshots of the main screens, at a phone-like size.
 * `./gradlew :app:shared:recordRoborazziJvm` writes them to app/shared/build/outputs/roborazzi/.
 */
@OptIn(ExperimentalTestApi::class)
class ScreenshotTest {
    @AfterTest
    fun tearDown() = stopTestKoin()

    private fun screenshot(
        name: String,
        roleHeld: Boolean = true,
        tab: String? = null,
    ) = runDesktopComposeUiTest(width = PHONE_WIDTH, height = PHONE_HEIGHT) {
        startTestKoin(roleHeld)
        setContent { App() }
        tab?.let { onNodeWithTag(it).performClick() }
        onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Test
    fun filtersWithFilteringOff() = screenshot("filters-filtering-off", roleHeld = false)

    @Test
    fun filters() = screenshot("filters")

    @Test
    fun history() = screenshot("history", tab = "tab.history")

    @Test
    fun stats() = screenshot("stats", tab = "tab.stats")

    private companion object {
        const val PHONE_WIDTH = 412
        const val PHONE_HEIGHT = 892
    }
}
