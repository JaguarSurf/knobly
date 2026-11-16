package app.knobly.feature.calls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.knobly.core.model.IncomingCall
import app.knobly.core.phone.PhoneNumberNormalizer
import app.knobly.domain.rules.RuleContext
import app.knobly.domain.rules.call.CallFilterSettingsRepository
import app.knobly.domain.rules.call.CustomRule
import app.knobly.domain.rules.call.CustomRuleError
import app.knobly.domain.rules.call.CustomRuleType
import app.knobly.domain.rules.call.CustomRuleValidator
import app.knobly.domain.rules.call.buildCustomRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** State of the "new rule" / "edit rule" screen. */
data class RuleEditorUiState(
    val isNew: Boolean = true,
    val type: CustomRuleType = CustomRuleType.PREFIX,
    val value: String = "",
    val label: String = "",
    /** The rule also rejects numbers saved in the contacts (not offered for hidden numbers). */
    val applyToContacts: Boolean = false,
    val error: CustomRuleError? = null,
    val testNumber: String = "",
    /** Result of trying [testNumber] against the rule being edited: `null` when there is nothing to show. */
    val testRejected: Boolean? = null,
    /** The screen should close (saved or deleted). */
    val done: Boolean = false,
)

/** Creates or edits one custom rule; [ruleId] `null` means a new rule. */
class RuleEditorViewModel(
    private val ruleId: String?,
    private val repository: CallFilterSettingsRepository,
    private val normalizer: PhoneNumberNormalizer,
) : ViewModel() {
    private val mutableState = MutableStateFlow(RuleEditorUiState(isNew = ruleId == null))
    val state: StateFlow<RuleEditorUiState> = mutableState.asStateFlow()
    private var existing: CustomRule? = null

    init {
        if (ruleId != null) {
            viewModelScope.launch {
                repository.rule(ruleId)?.let { rule ->
                    existing = rule
                    mutableState.update {
                        it.copy(
                            type = rule.type,
                            value = rule.value,
                            label = rule.label,
                            applyToContacts = rule.applyToContacts,
                        )
                    }
                }
            }
        }
    }

    fun setType(type: CustomRuleType) = edit { it.copy(type = type, error = null) }

    fun setValue(value: String) = edit { it.copy(value = value, error = null) }

    fun setLabel(label: String) = mutableState.update { it.copy(label = label) }

    fun setApplyToContacts(apply: Boolean) = mutableState.update { it.copy(applyToContacts = apply) }

    fun setTestNumber(number: String) = edit { it.copy(testNumber = number) }

    @OptIn(ExperimentalUuidApi::class)
    fun save() {
        val current = mutableState.value
        val error = CustomRuleValidator.validate(current.type, current.value, normalizer)
        if (error != null) {
            mutableState.update { it.copy(error = error) }
            return
        }
        val rule =
            CustomRule(
                id = existing?.id ?: Uuid.random().toString(),
                type = current.type,
                value = if (current.type == CustomRuleType.HIDDEN_NUMBER) "" else current.value.trim(),
                label = current.label.trim(),
                enabled = existing?.enabled ?: true,
                applyToContacts = current.applyToContacts && current.type != CustomRuleType.HIDDEN_NUMBER,
            )
        viewModelScope.launch {
            repository.saveRule(rule)
            mutableState.update { it.copy(done = true) }
        }
    }

    fun delete() {
        val id = existing?.id ?: return
        viewModelScope.launch {
            repository.deleteRule(id)
            mutableState.update { it.copy(done = true) }
        }
    }

    /** Applies [change] and re-evaluates the test number with the real rule implementation. */
    private fun edit(change: (RuleEditorUiState) -> RuleEditorUiState) {
        mutableState.update { state ->
            val next = change(state)
            next.copy(testRejected = evaluateTest(next))
        }
    }

    private fun evaluateTest(state: RuleEditorUiState): Boolean? {
        if (state.testNumber.isBlank() || state.type == CustomRuleType.HIDDEN_NUMBER) return null
        val call = IncomingCall(normalizer.normalize(state.testNumber), Clock.System.now())
        return buildCustomRule(CustomRule("test", state.type, state.value), priority = 0, normalizer)
            ?.matches(call, RuleContext())
    }
}
