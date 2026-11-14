package app.knobly.app.shared

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import app.knobly.domain.history.RejectedCall
import app.knobly.domain.rules.call.CallFilterSettings
import app.knobly.domain.rules.call.CustomRule
import app.knobly.domain.rules.call.CustomRuleType
import app.knobly.feature.calls.FiltersTestTags
import app.knobly.feature.history.HistoryTestTags
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.AfterTest
import kotlin.test.Test

/**
 * Roborazzi screenshots of the main screens (pt-BR, phone size, light and dark).
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
        dark: Boolean = false,
        settings: CallFilterSettings = CallFilterSettings(),
        contactsGranted: Boolean = true,
        history: List<RejectedCall> = emptyList(),
        thenClick: String? = null,
        then: ComposeUiTest.() -> Unit = {},
    ) = runDesktopComposeUiTest(width = PHONE_WIDTH, height = PHONE_HEIGHT) {
        startTestKoin(roleHeld, contactsGranted, settings, history)
        setContent { App(darkTheme = dark) }
        tab?.let { onNodeWithTag(it).performClick() }
        thenClick?.let { onNodeWithTag(it).performScrollTo().performClick() }
        then()
        // The whole scene, so sheets and dialogs (separate layers) are in the picture too.
        captureToImage().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Test
    fun home() = screenshot("home")

    @Test
    fun homeDark() = screenshot("home-dark", dark = true)

    @Test
    fun homeFilteringOff() = screenshot("home-filtering-off", roleHeld = false)

    @Test
    fun filtersFilteringOffDark() =
        screenshot("filters-filtering-off-dark", roleHeld = false, tab = "tab.filters", dark = true)

    @Test
    fun filters() = screenshot("filters", tab = "tab.filters")

    @Test
    fun filtersWithRules() = screenshot("filters-rules", tab = "tab.filters", settings = SAMPLE)

    @Test
    fun filtersWithRulesDark() = screenshot("filters-rules-dark", tab = "tab.filters", settings = SAMPLE, dark = true)

    @Test
    fun filtersContactsDenied() =
        screenshot(
            "filters-contacts-denied",
            tab = "tab.filters",
            settings = SAMPLE.copy(contactsOnly = true),
            contactsGranted = false,
        )

    @Test
    fun homeProtected() = screenshot("home-protected", settings = SAMPLE)

    @Test
    fun ruleEditorNew() = screenshot("rule-editor-new", tab = "tab.filters", thenClick = FiltersTestTags.NEW_RULE)

    @Test
    fun ruleEditorEditDark() =
        screenshot(
            "rule-editor-edit-dark",
            tab = "tab.filters",
            settings = SAMPLE,
            dark = true,
            thenClick = FiltersTestTags.rule("regex"),
        )

    @Test
    fun ruleEditorTryNumber() =
        screenshot(
            "rule-editor-try-number",
            tab = "tab.filters",
            settings = SAMPLE,
            thenClick = FiltersTestTags.rule("tm"),
        ) {
            onNode(
                hasSetTextAction() and hasAnyAncestor(hasTestTag(FiltersTestTags.EDITOR_TEST)),
            ).performTextInput("0303 123 4567")
        }

    @Test
    fun historyEmpty() = screenshot("history-empty", tab = "tab.history")

    @Test
    fun history() = screenshot("history", tab = "tab.history", history = SampleHistory.calls)

    @Test
    fun historyDark() = screenshot("history-dark", tab = "tab.history", history = SampleHistory.calls, dark = true)

    @Test
    fun historyDetailsDark() =
        screenshot(
            "history-details-dark",
            tab = "tab.history",
            history = SampleHistory.calls,
            dark = true,
            thenClick = HistoryTestTags.entry(1),
        )

    @Test
    fun historyClear() =
        screenshot(
            "history-clear",
            tab = "tab.history",
            history = SampleHistory.calls,
            thenClick = HistoryTestTags.CLEAR,
        )

    @Test
    fun statsEmpty() = screenshot("stats-empty", tab = "tab.stats")

    @Test
    fun stats() = screenshot("stats", tab = "tab.stats", history = SampleHistory.longHistory)

    @Test
    fun statsDark() = screenshot("stats-dark", tab = "tab.stats", history = SampleHistory.longHistory, dark = true)

    @Test
    fun statsHourDark() =
        screenshot("stats-hour-dark", tab = "tab.stats", history = SampleHistory.longHistory, dark = true) {
            onNodeWithText("Hora").performClick()
        }

    @Test
    fun statsWeek() =
        screenshot("stats-week", tab = "tab.stats", history = SampleHistory.longHistory) {
            onNodeWithText("Semana").performClick()
        }

    @Test
    fun statsMonth() =
        screenshot("stats-month", tab = "tab.stats", history = SampleHistory.longHistory) {
            onNodeWithText("Mês").performClick()
        }

    @Test
    fun statsYearDark() =
        screenshot("stats-year-dark", tab = "tab.stats", history = SampleHistory.longHistory, dark = true) {
            onNodeWithText("Ano").performClick()
        }

    @Test
    fun homeActivity() = screenshot("home-activity", settings = SAMPLE, history = SampleHistory.calls)

    @Test
    fun homeActivityDark() =
        screenshot("home-activity-dark", settings = SAMPLE, history = SampleHistory.calls, dark = true)

    private companion object {
        const val PHONE_WIDTH = 412
        const val PHONE_HEIGHT = 892

        val SAMPLE =
            CallFilterSettings(
                contactsOnly = false,
                customRulesEnabled = true,
                customRules =
                    listOf(
                        CustomRule("tm", CustomRuleType.PREFIX, "0303", "Telemarketing"),
                        CustomRule("hidden", CustomRuleType.HIDDEN_NUMBER),
                        CustomRule("exact", CustomRuleType.EXACT_NUMBER, "(11) 4002-8922", enabled = false),
                        CustomRule("regex", CustomRuleType.REGEX, "^0?30[34]", "Telemarketing (regex)"),
                    ),
            )
    }
}
