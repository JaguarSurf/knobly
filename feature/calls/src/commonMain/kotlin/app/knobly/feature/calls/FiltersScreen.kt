package app.knobly.feature.calls

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.knobly.core.designsystem.GlassCard
import app.knobly.core.designsystem.KnoblyChip
import app.knobly.core.designsystem.KnoblyChipRow
import app.knobly.core.designsystem.KnoblyIcons
import app.knobly.core.designsystem.KnoblyPrimaryButton
import app.knobly.core.designsystem.KnoblySecondaryButton
import app.knobly.core.designsystem.KnoblyStatus
import app.knobly.core.designsystem.KnoblyTheme
import app.knobly.core.designsystem.KnoblyToggle
import app.knobly.core.designsystem.KnoblyToggleRow
import app.knobly.domain.rules.call.CustomRule
import app.knobly.domain.rules.call.CustomRuleType
import app.knobly.feature.calls.resources.Res
import app.knobly.feature.calls.resources.filters_contacts_denied
import app.knobly.feature.calls.resources.filters_contacts_only_subtitle
import app.knobly.feature.calls.resources.filters_contacts_only_title
import app.knobly.feature.calls.resources.filters_custom_contacts_note
import app.knobly.feature.calls.resources.filters_custom_subtitle
import app.knobly.feature.calls.resources.filters_custom_title
import app.knobly.feature.calls.resources.filters_module_calls
import app.knobly.feature.calls.resources.filters_module_messages
import app.knobly.feature.calls.resources.filters_module_notifications
import app.knobly.feature.calls.resources.filters_new_rule
import app.knobly.feature.calls.resources.filters_no_rules
import app.knobly.feature.calls.resources.filters_no_rules_hint
import app.knobly.feature.calls.resources.filters_open_settings
import app.knobly.feature.calls.resources.filters_status_active
import app.knobly.feature.calls.resources.filters_status_none
import app.knobly.feature.calls.resources.filters_title
import app.knobly.feature.calls.resources.rule_type_hidden_description
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The Filters tab. [topContent] is placed above the filters by the app layer (the "call filtering is off" card
 * comes from another feature).
 */
@Composable
fun FiltersScreen(
    onNewRule: () -> Unit,
    onEditRule: (String) -> Unit,
    modifier: Modifier = Modifier,
    topContent: @Composable () -> Unit = {},
    viewModel: FiltersViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FiltersContent(
        state = state,
        onContactsOnlyChange = viewModel::setContactsOnly,
        onCustomRulesChange = viewModel::setCustomRulesEnabled,
        onRuleEnabledChange = viewModel::setRuleEnabled,
        onOpenContactsSettings = viewModel::openContactsSettings,
        onNewRule = onNewRule,
        onEditRule = onEditRule,
        topContent = topContent,
        modifier = modifier,
    )
}

@Composable
fun FiltersContent(
    state: FiltersUiState,
    onContactsOnlyChange: (Boolean) -> Unit,
    onCustomRulesChange: (Boolean) -> Unit,
    onRuleEnabledChange: (String, Boolean) -> Unit,
    onOpenContactsSettings: () -> Unit,
    onNewRule: () -> Unit,
    onEditRule: (String) -> Unit,
    modifier: Modifier = Modifier,
    topContent: @Composable () -> Unit = {},
) {
    val c = KnoblyTheme.colors
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState(),
                ).padding(horizontal = 18.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(Res.string.filters_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
            KnoblyStatus(
                text =
                    if (state.activeFilterCount > 0) {
                        pluralStringResource(
                            Res.plurals.filters_status_active,
                            state.activeFilterCount,
                            state.activeFilterCount,
                        )
                    } else {
                        stringResource(Res.string.filters_status_none)
                    },
                active = state.activeFilterCount > 0,
                framed = true,
            )
        }

        KnoblyChipRow {
            KnoblyChip(stringResource(Res.string.filters_module_calls), selected = true, onClick = {})
            KnoblyChip(
                stringResource(Res.string.filters_module_messages),
                selected = false,
                onClick = {},
                enabled = false,
            )
            KnoblyChip(
                stringResource(Res.string.filters_module_notifications),
                selected = false,
                onClick = {},
                enabled = false,
            )
        }

        topContent()

        GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
            KnoblyToggleRow(
                title = stringResource(Res.string.filters_contacts_only_title),
                subtitle = stringResource(Res.string.filters_contacts_only_subtitle),
                icon = KnoblyIcons.Person,
                checked = state.contactsOnly,
                onCheckedChange = onContactsOnlyChange,
                modifier = Modifier.testTag(FiltersTestTags.CONTACTS_ONLY),
            )
            if (state.showContactsPermissionMessage) {
                ContactsNotice(
                    text = stringResource(Res.string.filters_contacts_denied),
                    onOpenSettings = onOpenContactsSettings,
                    modifier = Modifier.testTag(FiltersTestTags.CONTACTS_PERMISSION_MESSAGE),
                )
            }
        }

        GlassCard(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 14.dp),
        ) {
            KnoblyToggleRow(
                title = stringResource(Res.string.filters_custom_title),
                subtitle = stringResource(Res.string.filters_custom_subtitle),
                icon = KnoblyIcons.Rules,
                checked = state.customRulesEnabled,
                onCheckedChange = onCustomRulesChange,
                modifier = Modifier.testTag(FiltersTestTags.CUSTOM_RULES),
            )
            if (state.showContactsNotProtectedNote) {
                ContactsNotice(
                    text = stringResource(Res.string.filters_custom_contacts_note),
                    // One "Open settings" button is enough when the contacts card already shows it.
                    onOpenSettings = onOpenContactsSettings.takeUnless { state.showContactsPermissionMessage },
                )
            }
            if (state.rules.isEmpty()) {
                HorizontalDivider(color = c.divider)
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(stringResource(Res.string.filters_no_rules), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(Res.string.filters_no_rules_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                state.rules.forEach { rule ->
                    HorizontalDivider(color = c.divider)
                    RuleRow(
                        rule = rule,
                        groupEnabled = state.customRulesEnabled,
                        onEnabledChange = { onRuleEnabledChange(rule.id, it) },
                        onClick = { onEditRule(rule.id) },
                    )
                }
            }
            KnoblyPrimaryButton(
                text = stringResource(Res.string.filters_new_rule),
                onClick = onNewRule,
                icon = KnoblyIcons.Plus,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag(FiltersTestTags.NEW_RULE),
            )
        }
    }
}

@Composable
private fun RuleRow(
    rule: CustomRule,
    groupEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    val c = KnoblyTheme.colors
    val name = rule.displayName()
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .alpha(if (groupEnabled) 1f else 0.5f)
                .padding(vertical = 10.dp)
                .testTag(FiltersTestTags.rule(rule.id)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (rule.enabled && groupEnabled) c.accent else c.textSecondary.copy(alpha = 0.4f)),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                name,
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = MaterialTheme.typography.labelSmall.fontWeight,
                    ),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Without a nickname the title is already the type, so the subtitle shows only what it matches.
                val typeText =
                    when {
                        rule.label.isNotBlank() -> {
                            stringResource(rule.type.title)
                        }

                        rule.type == CustomRuleType.HIDDEN_NUMBER -> {
                            stringResource(
                                Res.string.rule_type_hidden_description,
                            )
                        }

                        else -> {
                            null
                        }
                    }
                typeText?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = c.textSecondary) }
                if (rule.value.isNotBlank()) {
                    Text(rule.value, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                }
            }
        }
        KnoblyToggle(
            checked = rule.enabled,
            onCheckedChange = onEnabledChange,
            enabled = groupEnabled,
            modifier = Modifier.semantics { contentDescription = name }.testTag(FiltersTestTags.ruleToggle(rule.id)),
        )
    }
}

@Composable
private fun ContactsNotice(
    text: String,
    onOpenSettings: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val c = KnoblyTheme.colors
    Column(modifier.fillMaxWidth().padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = c.alert)
        if (onOpenSettings != null) {
            KnoblySecondaryButton(
                stringResource(Res.string.filters_open_settings),
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
