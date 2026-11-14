package app.knobly.app.shared

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import app.knobly.domain.rules.call.CallFilterSettings
import app.knobly.domain.rules.call.CustomRule
import app.knobly.domain.rules.call.CustomRuleType
import app.knobly.feature.calls.FiltersTestTags
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
        thenClick: String? = null,
        then: ComposeUiTest.() -> Unit = {},
    ) = runDesktopComposeUiTest(width = PHONE_WIDTH, height = PHONE_HEIGHT) {
        startTestKoin(roleHeld, contactsGranted, settings)
        setContent { App(darkTheme = dark) }
        tab?.let { onNodeWithTag(it).performClick() }
        thenClick?.let { onNodeWithTag(it).performScrollTo().performClick() }
        then()
        onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
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
    fun history() = screenshot("history", tab = "tab.history")

    @Test
    fun stats() = screenshot("stats", tab = "tab.stats")

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
