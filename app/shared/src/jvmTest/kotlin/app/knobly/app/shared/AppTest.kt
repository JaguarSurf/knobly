package app.knobly.app.shared

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import app.knobly.domain.rules.call.CallFilterSettings
import app.knobly.domain.rules.call.CustomRule
import app.knobly.domain.rules.call.CustomRuleType
import app.knobly.feature.calls.FiltersTestTags
import app.knobly.feature.home.HomeTestTags
import app.knobly.feature.onboarding.CallFilteringTestTags
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
            onNodeWithTag(FiltersTestTags.CONTACTS_ONLY).assertIsDisplayed()

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
            // Screening is on, but nothing is filtered until a filter is switched on.
            onAllNodesWithText("Nenhum filtro ativo").onFirst().assertIsDisplayed()
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

    @Test
    fun `a custom rule is created in the editor and counted on home`() =
        runComposeUiTest {
            val env = startTestKoin(roleHeld = true, settings = CallFilterSettings(customRulesEnabled = true))
            setContent { App() }

            onNodeWithTag("tab.filters").performClick()
            onNodeWithTag(FiltersTestTags.NEW_RULE).performScrollTo().performClick()
            textField(FiltersTestTags.EDITOR_VALUE).performTextInput("0303")
            textField(FiltersTestTags.EDITOR_LABEL).performTextInput("Telemarketing")
            textField(FiltersTestTags.EDITOR_TEST).performTextInput("0303 123 4567")
            onNodeWithText("Seria rejeitada").assertIsDisplayed()
            onNodeWithTag(FiltersTestTags.EDITOR_SAVE).performClick()

            val rule =
                env.settings.value.customRules
                    .single()
            assertEquals(CustomRule(rule.id, CustomRuleType.PREFIX, "0303", "Telemarketing"), rule)
            onNodeWithTag(FiltersTestTags.rule(rule.id)).performScrollTo().assertIsDisplayed()

            onNodeWithTag("tab.home").performClick()
            onNodeWithText("1 filtro ativo", substring = true).assertIsDisplayed()
        }

    @Test
    fun `a rule can be switched off and deleted`() =
        runComposeUiTest {
            val rule = CustomRule("r1", CustomRuleType.PREFIX, "0303", "Telemarketing")
            val env =
                startTestKoin(
                    roleHeld = true,
                    settings = CallFilterSettings(customRulesEnabled = true, customRules = listOf(rule)),
                )
            setContent { App() }

            onNodeWithTag("tab.filters").performClick()
            onNodeWithTag(FiltersTestTags.ruleToggle("r1")).performScrollTo().performClick()
            assertFalse(
                env.settings.value.customRules
                    .single()
                    .enabled,
            )

            onNodeWithTag(FiltersTestTags.rule("r1")).performClick()
            onNodeWithTag(FiltersTestTags.EDITOR_DELETE).performClick()

            assertTrue(
                env.settings.value.customRules
                    .isEmpty(),
            )
            onNodeWithTag(FiltersTestTags.CONTACTS_ONLY).assertIsDisplayed()
        }

    @Test
    fun `contacts only stays off and offers the settings when the permission is denied`() =
        runComposeUiTest {
            val env = startTestKoin(roleHeld = true, contactsGranted = false)
            env.contacts.grantOnRequest = false
            setContent { App() }

            onNodeWithTag("tab.filters").performClick()
            onNodeWithTag(FiltersTestTags.CONTACTS_ONLY).performClick()

            assertFalse(env.settings.value.contactsOnly)
            onNodeWithTag(FiltersTestTags.CONTACTS_PERMISSION_MESSAGE).assertIsDisplayed()
            onNodeWithText("Abrir configurações").performClick()
            assertEquals(1, env.contacts.settingsOpened)
        }

    private fun ComposeUiTest.textField(tag: String) = onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(tag)))
}
