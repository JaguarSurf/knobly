package app.knobly.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Every design system component on one screen, with working state. Used for screenshot tests and for reviewing
 * the identity on a device or in the desktop harness.
 */
@Composable
fun DesignSystemCatalog(modifier: Modifier = Modifier) {
    val c = KnoblyTheme.colors
    var contacts by remember { mutableStateOf(true) }
    var rules by remember { mutableStateOf(false) }
    var chip by remember { mutableIntStateOf(0) }
    var period by remember { mutableIntStateOf(1) }
    var mode by remember { mutableIntStateOf(0) }
    var tab by remember { mutableIntStateOf(1) }

    KnoblyBackground(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                KnoblyMark(size = 44.dp)
                Column {
                    Text("knobly", style = MaterialTheme.typography.headlineMedium)
                    Text("Só toca quem você quer.", style = MaterialTheme.typography.bodySmall, color = c.accent)
                }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                    KnoblyStatus("2 filtros ativos", active = true, framed = true)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Chamadas", "Mensagens", "Notificações").forEachIndexed { index, label ->
                    KnoblyChip(label, selected = chip == index, onClick = { chip = index }, enabled = index == 0)
                }
            }

            GlassCard(
                Modifier.fillMaxWidth(),
                contentPadding =
                    androidx.compose.foundation.layout
                        .PaddingValues(horizontal = 18.dp, vertical = 4.dp),
            ) {
                KnoblyToggleRow(
                    title = "Aceitar somente contatos",
                    subtitle = "Rejeita quem não está na agenda, inclusive números ocultos.",
                    icon = KnoblyIcons.Person,
                    checked = contacts,
                    onCheckedChange = { contacts = it },
                )
                HorizontalDivider(color = c.divider)
                KnoblyToggleRow(
                    title = "Regras personalizadas",
                    subtitle = "Contatos sempre passam.",
                    icon = KnoblyIcons.Rules,
                    checked = rules,
                    onCheckedChange = { rules = it },
                )
            }

            KnoblySegmentedControl(
                options = listOf("Hora", "Dia", "Semana", "Mês", "Ano"),
                selectedIndex = period,
                onSelect = { period = it },
                modifier = Modifier.fillMaxWidth(),
            )

            GlassCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf(
                        "Começa com" to KnoblyIcons.Prefix,
                        "Número" to KnoblyIcons.Phone,
                        "Oculto" to KnoblyIcons.Hidden,
                        "Regex" to KnoblyIcons.Regex,
                    ).forEachIndexed { index, (label, icon) ->
                        KnoblyModeButton(label, icon, selected = mode == index, onClick = { mode = index })
                    }
                }
            }

            KnoblyPrimaryButton("Ativar filtro de chamadas", onClick = {}, modifier = Modifier.fillMaxWidth())
            KnoblySecondaryButton(
                "Criar regra",
                onClick = {},
                icon = KnoblyIcons.Plus,
                modifier = Modifier.fillMaxWidth(),
            )

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Bottom) {
                KnoblyNavBar(
                    items =
                        listOf(
                            KnoblyNavItem("Início", KnoblyIcons.Home, "nav.home"),
                            KnoblyNavItem("Filtros", KnoblyIcons.Filters, "nav.filters"),
                            KnoblyNavItem("Histórico", KnoblyIcons.History, "nav.history"),
                            KnoblyNavItem("Estatísticas", KnoblyIcons.Stats, "nav.stats"),
                        ),
                    selectedIndex = tab,
                    onSelect = { tab = it },
                )
            }
        }
    }
}

@Preview
@Composable
private fun DesignSystemCatalogDarkPreview() {
    KnoblyTheme(darkTheme = true) { DesignSystemCatalog() }
}

@Preview
@Composable
private fun DesignSystemCatalogLightPreview() {
    KnoblyTheme(darkTheme = false) { DesignSystemCatalog() }
}
