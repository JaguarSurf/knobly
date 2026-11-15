package app.knobly.app.shared

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.knobly.domain.stats.CallCategory
import app.knobly.feature.history.HistoryFilter
import app.knobly.feature.history.HistoryTestTags
import app.knobly.feature.stats.StatsTestTags
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Small phones and large fonts: pills keep their text on one line (a chip that does not fit moves to the next
 * line instead). 384 dp is a Galaxy A15 (1080 x 2340 px at 450 dpi), where "Rejeitadas" broke into "Rejeitada/s".
 */
@OptIn(ExperimentalTestApi::class)
class SmallScreenTest {
    @AfterTest
    fun tearDown() = stopTestKoin()

    private fun onSmallPhone(
        width: Int,
        fontScale: Float,
        block: ComposeUiTest.() -> Unit,
    ) = runDesktopComposeUiTest(width = width, height = 800) {
        startTestKoin(roleHeld = true, history = SampleHistory.mixed)
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) { App() }
        }
        block()
    }

    private fun SemanticsNodeInteraction.assertOneLinePill() {
        val bounds = getBoundsInRoot()
        assertEquals(PILL_HEIGHT, bounds.bottom - bounds.top)
    }

    @Test
    fun `history filters stay on one line at 384 dp`() =
        onSmallPhone(width = 384, fontScale = 1f) {
            onNodeWithTag("tab.history").performClick()
            HistoryFilter.entries.forEach { onNodeWithTag(HistoryTestTags.filter(it)).assertOneLinePill() }
        }

    @Test
    fun `history filters stay on one line at 360 dp`() =
        onSmallPhone(width = 360, fontScale = 1f) {
            onNodeWithTag("tab.history").performClick()
            HistoryFilter.entries.forEach { onNodeWithTag(HistoryTestTags.filter(it)).assertOneLinePill() }
        }

    @Test
    fun `history filters stay on one line at 320 dp with a large font`() =
        onSmallPhone(width = 320, fontScale = 1.3f) {
            onNodeWithTag("tab.history").performClick()
            HistoryFilter.entries.forEach { onNodeWithTag(HistoryTestTags.filter(it)).assertOneLinePill() }
        }

    @Test
    fun `stats categories stay on one line at 320 dp with a large font`() =
        onSmallPhone(width = 320, fontScale = 1.3f) {
            onNodeWithTag("tab.stats").performClick()
            CallCategory.entries.forEach { onNodeWithTag(StatsTestTags.category(it)).assertOneLinePill() }
        }

    private companion object {
        val PILL_HEIGHT = 40.dp
    }
}
