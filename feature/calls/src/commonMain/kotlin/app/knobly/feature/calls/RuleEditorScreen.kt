package app.knobly.feature.calls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.knobly.core.designsystem.GlassCard
import app.knobly.core.designsystem.KnoblyIcons
import app.knobly.core.designsystem.KnoblyModeButton
import app.knobly.core.designsystem.KnoblyPrimaryButton
import app.knobly.core.designsystem.KnoblySecondaryButton
import app.knobly.core.designsystem.KnoblyTextField
import app.knobly.core.designsystem.KnoblyTheme
import app.knobly.core.designsystem.KnoblyToggleRow
import app.knobly.domain.rules.call.CustomRuleType
import app.knobly.feature.calls.resources.Res
import app.knobly.feature.calls.resources.rule_editor_apply_to_contacts
import app.knobly.feature.calls.resources.rule_editor_apply_to_contacts_help
import app.knobly.feature.calls.resources.rule_editor_back
import app.knobly.feature.calls.resources.rule_editor_delete
import app.knobly.feature.calls.resources.rule_editor_edit_title
import app.knobly.feature.calls.resources.rule_editor_help_exact
import app.knobly.feature.calls.resources.rule_editor_help_hidden
import app.knobly.feature.calls.resources.rule_editor_help_prefix
import app.knobly.feature.calls.resources.rule_editor_help_regex
import app.knobly.feature.calls.resources.rule_editor_label
import app.knobly.feature.calls.resources.rule_editor_new_title
import app.knobly.feature.calls.resources.rule_editor_save
import app.knobly.feature.calls.resources.rule_editor_test
import app.knobly.feature.calls.resources.rule_editor_test_allowed
import app.knobly.feature.calls.resources.rule_editor_test_placeholder
import app.knobly.feature.calls.resources.rule_editor_test_rejected
import app.knobly.feature.calls.resources.rule_editor_type
import app.knobly.feature.calls.resources.rule_editor_value_exact
import app.knobly.feature.calls.resources.rule_editor_value_prefix
import app.knobly.feature.calls.resources.rule_editor_value_regex
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Creates ([ruleId] `null`) or edits a custom rule; calls [onClose] when saved, deleted or dismissed. */
@Composable
fun RuleEditorScreen(
    ruleId: String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RuleEditorViewModel = koinViewModel(key = "rule-editor-$ruleId") { parametersOf(ruleId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onClose() }
    RuleEditorContent(
        state = state,
        onTypeChange = viewModel::setType,
        onValueChange = viewModel::setValue,
        onLabelChange = viewModel::setLabel,
        onApplyToContactsChange = viewModel::setApplyToContacts,
        onTestNumberChange = viewModel::setTestNumber,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
        onBack = onClose,
        modifier = modifier,
    )
}

@Composable
fun RuleEditorContent(
    state: RuleEditorUiState,
    onTypeChange: (CustomRuleType) -> Unit,
    onValueChange: (String) -> Unit,
    onLabelChange: (String) -> Unit,
    onApplyToContactsChange: (Boolean) -> Unit,
    onTestNumberChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = KnoblyTheme.colors
    // imePadding: the keyboard must not cover the test field or the save button.
    Column(modifier.fillMaxSize().imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(c.glass)
                    .border(1.dp, c.glassBorder, CircleShape)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = stringResource(Res.string.rule_editor_back),
                        onClick = onBack,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    KnoblyIcons.Back,
                    contentDescription = stringResource(Res.string.rule_editor_back),
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                stringResource(
                    if (state.isNew) Res.string.rule_editor_new_title else Res.string.rule_editor_edit_title,
                ),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 14.dp),
            )
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            GlassCard(
                Modifier.fillMaxWidth(),
                cornerRadius = 26.dp,
                contentPadding = PaddingValues(vertical = 18.dp, horizontal = 10.dp),
            ) {
                Text(
                    stringResource(Res.string.rule_editor_type),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 8.dp, bottom = 14.dp),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    CustomRuleType.entries.forEach { type ->
                        KnoblyModeButton(
                            label = stringResource(type.shortTitle),
                            icon = type.icon,
                            selected = state.type == type,
                            onClick = { onTypeChange(type) },
                            modifier = Modifier.testTag(FiltersTestTags.editorType(type.name)),
                        )
                    }
                }
            }

            if (state.type == CustomRuleType.HIDDEN_NUMBER) {
                GlassCard(Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                    Text(
                        stringResource(Res.string.rule_editor_help_hidden),
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textSoft,
                    )
                }
            } else {
                RuleValueField(state, onValueChange)
            }

            KnoblyTextField(
                value = state.label,
                onValueChange = onLabelChange,
                label = stringResource(Res.string.rule_editor_label),
                modifier = Modifier.testTag(FiltersTestTags.EDITOR_LABEL),
            )

            // A hidden number is never a contact, so the option would do nothing there.
            if (state.type != CustomRuleType.HIDDEN_NUMBER) {
                GlassCard(
                    Modifier.fillMaxWidth(),
                    cornerRadius = 22.dp,
                    contentPadding = PaddingValues(horizontal = 18.dp),
                ) {
                    KnoblyToggleRow(
                        title = stringResource(Res.string.rule_editor_apply_to_contacts),
                        subtitle = stringResource(Res.string.rule_editor_apply_to_contacts_help),
                        icon = KnoblyIcons.Person,
                        checked = state.applyToContacts,
                        onCheckedChange = onApplyToContactsChange,
                        modifier = Modifier.testTag(FiltersTestTags.EDITOR_APPLY_TO_CONTACTS),
                    )
                }
            }

            if (state.type != CustomRuleType.HIDDEN_NUMBER) {
                GlassCard(Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
                    // The result sits on the label line, so the keyboard never hides it while typing.
                    Box(Modifier.fillMaxWidth()) {
                        KnoblyTextField(
                            value = state.testNumber,
                            onValueChange = onTestNumberChange,
                            label = stringResource(Res.string.rule_editor_test),
                            placeholder = stringResource(Res.string.rule_editor_test_placeholder),
                            keyboardType = KeyboardType.Phone,
                            modifier = Modifier.testTag(FiltersTestTags.EDITOR_TEST),
                        )
                        state.testRejected?.let { rejected ->
                            TestResult(rejected, Modifier.align(Alignment.TopEnd).offset(y = (-4).dp))
                        }
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            KnoblyPrimaryButton(
                text = stringResource(Res.string.rule_editor_save),
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().testTag(FiltersTestTags.EDITOR_SAVE),
            )
            if (!state.isNew) {
                KnoblySecondaryButton(
                    text = stringResource(Res.string.rule_editor_delete),
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth().testTag(FiltersTestTags.EDITOR_DELETE),
                )
            }
        }
    }
}

@Composable
private fun TestResult(
    rejected: Boolean,
    modifier: Modifier = Modifier,
) {
    val c = KnoblyTheme.colors
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier
            .clip(shape)
            .background(if (rejected) c.glassStrong else c.glass)
            .border(1.dp, if (rejected) c.accent else c.glassBorder, shape)
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .testTag(FiltersTestTags.EDITOR_TEST_RESULT),
    ) {
        Text(
            stringResource(if (rejected) Res.string.rule_editor_test_rejected else Res.string.rule_editor_test_allowed),
            style = MaterialTheme.typography.labelMedium,
            color = if (rejected) c.accent else c.textSecondary,
        )
    }
}

/** The value of a prefix, exact number or regex rule, with help and errors for the selected type. */
@Composable
private fun RuleValueField(
    state: RuleEditorUiState,
    onValueChange: (String) -> Unit,
) {
    KnoblyTextField(
        value = state.value,
        onValueChange = onValueChange,
        label =
            stringResource(
                when (state.type) {
                    CustomRuleType.EXACT_NUMBER -> Res.string.rule_editor_value_exact
                    CustomRuleType.REGEX -> Res.string.rule_editor_value_regex
                    else -> Res.string.rule_editor_value_prefix
                },
            ),
        help =
            stringResource(
                when (state.type) {
                    CustomRuleType.EXACT_NUMBER -> Res.string.rule_editor_help_exact
                    CustomRuleType.REGEX -> Res.string.rule_editor_help_regex
                    else -> Res.string.rule_editor_help_prefix
                },
            ),
        error = state.error?.let { stringResource(it.message) },
        keyboardType = if (state.type == CustomRuleType.REGEX) KeyboardType.Ascii else KeyboardType.Phone,
        monospace = true,
        modifier = Modifier.testTag(FiltersTestTags.EDITOR_VALUE),
    )
}
