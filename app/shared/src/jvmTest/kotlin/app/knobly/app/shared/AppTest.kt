package app.knobly.app.shared

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import app.knobly.feature.onboarding.CallFilteringTestTags
import kotlin.test.AfterTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AppTest {
    @AfterTest
    fun tearDown() = stopTestKoin()

    @Test
    fun `starts on the filters tab and navigates between the three tabs`() =
        runComposeUiTest {
            startTestKoin(roleHeld = true)
            setContent { App() }

            onNodeWithTag("tab.filters").assertIsSelected()
            onNodeWithText("Your call filter rules will be listed here.").assertIsDisplayed()

            onNodeWithTag("tab.history").performClick()
            onNodeWithTag("tab.history").assertIsSelected()
            onNodeWithText("Calls filtered by Knobly will appear here.").assertIsDisplayed()

            onNodeWithTag("tab.stats").performClick()
            onNodeWithTag("tab.stats").assertIsSelected()
            onNodeWithText("Charts of filtered calls", substring = true).assertIsDisplayed()
        }

    @Test
    fun `opens directly on the tabs even when call filtering is off`() =
        runComposeUiTest {
            startTestKoin(roleHeld = false)
            setContent { App() }

            onNodeWithTag("tab.filters").assertIsSelected()
            onNodeWithTag(CallFilteringTestTags.CARD).assertIsDisplayed()
        }

    @Test
    fun `enabling call filtering from the filters tab hides the card`() =
        runComposeUiTest {
            startTestKoin(roleHeld = false)
            setContent { App() }

            onNodeWithTag(CallFilteringTestTags.ENABLE).performClick()

            onNodeWithTag(CallFilteringTestTags.CARD).assertDoesNotExist()
            onNodeWithTag("tab.filters").assertIsSelected()
        }

    @Test
    fun `no call filtering card when filtering is already on`() =
        runComposeUiTest {
            startTestKoin(roleHeld = true)
            setContent { App() }

            onNodeWithTag(CallFilteringTestTags.CARD).assertDoesNotExist()
        }
}
