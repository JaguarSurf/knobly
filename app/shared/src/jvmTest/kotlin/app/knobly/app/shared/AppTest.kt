package app.knobly.app.shared

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import app.knobly.feature.home.HomeTestTags
import app.knobly.feature.onboarding.CallFilteringTestTags
import kotlin.test.AfterTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AppTest {
    @AfterTest
    fun tearDown() = stopTestKoin()

    @Test
    fun `starts on the home tab and navigates between the four tabs`() =
        runComposeUiTest {
            startTestKoin(roleHeld = true)
            setContent { App() }

            onNodeWithTag("tab.home").assertIsSelected()
            onNodeWithText("sob controle.", substring = true).assertIsDisplayed()

            onNodeWithTag("tab.filters").performClick()
            onNodeWithTag("tab.filters").assertIsSelected()
            onNodeWithText("Suas regras de filtro de chamadas aparecerão aqui.").assertIsDisplayed()

            onNodeWithTag("tab.history").performClick()
            onNodeWithTag("tab.history").assertIsSelected()
            onNodeWithText("As chamadas filtradas pelo Knobly aparecerão aqui.").assertIsDisplayed()

            onNodeWithTag("tab.stats").performClick()
            onNodeWithTag("tab.stats").assertIsSelected()
            onNodeWithText("Gráficos das chamadas filtradas", substring = true).assertIsDisplayed()
        }

    @Test
    fun `the calls tile on home opens the filters tab`() =
        runComposeUiTest {
            startTestKoin(roleHeld = true)
            setContent { App() }

            onNodeWithTag(HomeTestTags.CALLS_TILE).performClick()

            onNodeWithTag("tab.filters").assertIsSelected()
        }

    @Test
    fun `call filtering can be enabled from home`() =
        runComposeUiTest {
            startTestKoin(roleHeld = false)
            setContent { App() }

            onAllNodesWithText("Desligado").onFirst().assertIsDisplayed()
            onNodeWithTag(HomeTestTags.ENABLE_CALL_FILTERING).performClick()

            onNodeWithTag(HomeTestTags.ENABLE_CALL_FILTERING).assertDoesNotExist()
            onAllNodesWithText("Protegido").onFirst().assertIsDisplayed()
        }

    @Test
    fun `filters tab shows the call filtering card until filtering is enabled`() =
        runComposeUiTest {
            startTestKoin(roleHeld = false)
            setContent { App() }

            onNodeWithTag("tab.filters").performClick()
            onNodeWithTag(CallFilteringTestTags.CARD).assertIsDisplayed()

            onNodeWithTag(CallFilteringTestTags.ENABLE).performClick()

            onNodeWithTag(CallFilteringTestTags.CARD).assertDoesNotExist()
        }
}
