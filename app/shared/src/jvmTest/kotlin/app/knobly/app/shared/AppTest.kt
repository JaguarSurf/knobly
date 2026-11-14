package app.knobly.app.shared

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithTag
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
import app.knobly.feature.history.HistoryFilter
import app.knobly.feature.history.HistoryTestTags
import app.knobly.feature.home.HomeTestTags
import app.knobly.feature.onboarding.CallFilteringTestTags
import app.knobly.feature.stats.StatsTestTags
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
            onNodeWithText("Nenhuma chamada rejeitada").assertIsDisplayed()

            onNodeWithTag("tab.stats").performClick()
            onNodeWithTag("tab.stats").assertIsSelected()
            onNodeWithTag(StatsTestTags.TOTAL).assertTextContains("nos últimos 7 dias", substring = true)
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

    @Test
    fun `history lists rejected calls by day and filters them`() =
        runComposeUiTest {
            startTestKoin(roleHeld = true, history = SampleHistory.calls)
            setContent { App() }
            onNodeWithTag("tab.history").performClick()

            onNodeWithText("HOJE").assertIsDisplayed()
            onNodeWithText("0303 123 4567").assertIsDisplayed()
            onNodeWithTag(HistoryTestTags.entry(1))
                .assertTextContains("Telemarketing · começa com 0303")
                .assertTextContains("13:32")
            onNodeWithText("Número oculto").assertIsDisplayed()

            onNodeWithTag(HistoryTestTags.filter(HistoryFilter.RULES)).performClick()
            onNodeWithText("Número oculto").assertDoesNotExist()
            onNodeWithText("0303 123 4567").assertIsDisplayed()

            onNodeWithTag(HistoryTestTags.filter(HistoryFilter.CONTACTS_ONLY)).performClick()
            onNodeWithText("0303 123 4567").assertDoesNotExist()
            onNodeWithText("(21) 99876-5432").assertIsDisplayed()
        }

    @Test
    fun `a history entry opens details and can be deleted`() =
        runComposeUiTest {
            val env = startTestKoin(roleHeld = true, history = SampleHistory.calls)
            setContent { App() }
            onNodeWithTag("tab.history").performClick()

            onNodeWithTag(HistoryTestTags.entry(1)).performClick()
            onNodeWithTag(HistoryTestTags.DETAILS).assertIsDisplayed()
            onNodeWithText("14 de novembro de 2026 às 13:32").assertIsDisplayed()
            onNodeWithTag(HistoryTestTags.COPY).performClick()
            onNodeWithText("Número copiado").assertIsDisplayed()

            onNodeWithTag(HistoryTestTags.DELETE).performClick()

            onNodeWithTag(HistoryTestTags.DETAILS).assertDoesNotExist()
            assertTrue(env.history.value.none { it.id == 1L })
            onNodeWithText("0303 123 4567").assertDoesNotExist()
        }

    @Test
    fun `clearing the history asks for confirmation`() =
        runComposeUiTest {
            val env = startTestKoin(roleHeld = true, history = SampleHistory.calls)
            setContent { App() }
            onNodeWithTag("tab.history").performClick()

            onNodeWithTag(HistoryTestTags.CLEAR).performClick()
            onNodeWithText("As 7 chamadas rejeitadas serão apagadas deste aparelho.").assertIsDisplayed()
            onNodeWithText("Cancelar").performClick()
            assertEquals(7, env.history.value.size)

            onNodeWithTag(HistoryTestTags.CLEAR).performClick()
            onNodeWithTag(HistoryTestTags.CLEAR_CONFIRM).performClick()

            assertTrue(env.history.value.isEmpty())
            onNodeWithText("Nenhuma chamada rejeitada").assertIsDisplayed()
            onNodeWithTag(HistoryTestTags.CLEAR).assertDoesNotExist()
        }

    @Test
    fun `statistics show the rejected calls of each period`() =
        runComposeUiTest {
            startTestKoin(roleHeld = true, history = SampleHistory.calls)
            setContent { App() }
            onNodeWithTag("tab.stats").performClick()

            // Last 7 days: all 7 sample calls; 4 rejected by contacts only, 2 by Telemarketing.
            onNodeWithTag(
                StatsTestTags.TOTAL,
            ).assertTextContains("7").assertTextContains("rejeitadas nos últimos 7 dias")
            onNodeWithText("Fora dos contatos").assertIsDisplayed()
            onNodeWithText("Telemarketing · 0303").assertIsDisplayed()

            onNodeWithText("Hora").performClick()
            // Last 24 hours: 13:32, 11:05, 09:47 today and 19:10, 15:22 yesterday.
            onNodeWithTag(
                StatsTestTags.TOTAL,
            ).assertTextContains("5").assertTextContains("rejeitadas nas últimas 24 horas")
        }

    @Test
    fun `home shows today's rejections and opens the latest in history`() =
        runComposeUiTest {
            startTestKoin(roleHeld = true, history = SampleHistory.calls)
            setContent { App() }

            onAllNodesWithTag(HomeTestTags.STATS)[0].assertTextContains("3 hoje")
            onAllNodesWithTag(HomeTestTags.STATS)[1].assertTextContains("7 em 7 dias")
            onNodeWithTag(HomeTestTags.LATEST_REJECTION).performScrollTo()
            onNodeWithText("Rejeitada hoje às 13:32").assertIsDisplayed()

            onNodeWithTag(HomeTestTags.LATEST_REJECTION).performClick()
            onNodeWithTag("tab.history").assertIsSelected()
        }

    private fun ComposeUiTest.textField(tag: String) = onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(tag)))
}
