package app.knobly.app.shared

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.knobly.core.designsystem.KnoblyChipRow
import app.knobly.domain.stats.CallCategory
import app.knobly.feature.history.HistoryFilter
import app.knobly.feature.history.HistoryTestTags
import app.knobly.feature.stats.StatsTestTags
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Small phones and large fonts: pills never break their text (always 40 dp high), and a [KnoblyChipRow] keeps them
 * on one line down to 360 dp. 384 dp is a Galaxy A15 (1080 x 2340 px at 450 dpi), where "Rejeitadas" broke into
 * "Rejeitada/s"; 360 dp is a 720 x 1600 px phone at density 2.0.
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

    private fun ComposeUiTest.pills(tags: List<String>) = tags.map { onNodeWithTag(it).getBoundsInRoot() }

    /** Every pill keeps its text on one line. */
    private fun ComposeUiTest.assertWholePills(tags: List<String>) =
        pills(tags).forEach { assertEquals(PILL_HEIGHT, it.bottom - it.top) }

    /** Whole pills, side by side on a single line. */
    private fun ComposeUiTest.assertPillsInOneLine(tags: List<String>) {
        assertWholePills(tags)
        assertEquals(1, pills(tags).map { it.top }.distinct().size, "pills on more than one line")
    }

    private val historyFilters = HistoryFilter.entries.map(HistoryTestTags::filter)
    private val statsCategories = CallCategory.entries.map(StatsTestTags::category)

    @Test
    fun `history filters fit in one line on a Galaxy A15`() =
        onSmallPhone(width = 384, fontScale = 1f) {
            onNodeWithTag("tab.history").performClick()
            assertPillsInOneLine(historyFilters)
        }

    @Test
    fun `history filters fit in one line at 360 dp`() =
        onSmallPhone(width = 360, fontScale = 1f) {
            onNodeWithTag("tab.history").performClick()
            assertPillsInOneLine(historyFilters)
        }

    @Test
    fun `stats categories fit in one line at 360 dp`() =
        onSmallPhone(width = 360, fontScale = 1f) {
            onNodeWithTag("tab.stats").performClick()
            assertPillsInOneLine(statsCategories)
        }

    @Test
    fun `history filters stay whole at 320 dp with a large font`() =
        onSmallPhone(width = 320, fontScale = 1.3f) {
            onNodeWithTag("tab.history").performClick()
            assertWholePills(historyFilters)
        }

    @Test
    fun `stats categories stay whole at 320 dp with a large font`() =
        onSmallPhone(width = 320, fontScale = 1.3f) {
            onNodeWithTag("tab.stats").performClick()
            assertWholePills(statsCategories)
        }

    private companion object {
        val PILL_HEIGHT = 40.dp
    }
}
