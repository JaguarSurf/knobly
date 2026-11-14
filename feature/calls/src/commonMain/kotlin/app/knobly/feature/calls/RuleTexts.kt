package app.knobly.feature.calls

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import app.knobly.core.designsystem.KnoblyIcons
import app.knobly.domain.rules.call.CustomRule
import app.knobly.domain.rules.call.CustomRuleError
import app.knobly.domain.rules.call.CustomRuleType
import app.knobly.feature.calls.resources.Res
import app.knobly.feature.calls.resources.rule_error_empty
import app.knobly.feature.calls.resources.rule_error_invalid_number
import app.knobly.feature.calls.resources.rule_error_invalid_prefix
import app.knobly.feature.calls.resources.rule_error_invalid_regex
import app.knobly.feature.calls.resources.rule_error_too_long
import app.knobly.feature.calls.resources.rule_type_exact
import app.knobly.feature.calls.resources.rule_type_exact_short
import app.knobly.feature.calls.resources.rule_type_hidden
import app.knobly.feature.calls.resources.rule_type_hidden_short
import app.knobly.feature.calls.resources.rule_type_prefix
import app.knobly.feature.calls.resources.rule_type_prefix_short
import app.knobly.feature.calls.resources.rule_type_regex
import app.knobly.feature.calls.resources.rule_type_regex_short
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal val CustomRuleType.title: StringResource
    get() =
        when (this) {
            CustomRuleType.PREFIX -> Res.string.rule_type_prefix
            CustomRuleType.EXACT_NUMBER -> Res.string.rule_type_exact
            CustomRuleType.HIDDEN_NUMBER -> Res.string.rule_type_hidden
            CustomRuleType.REGEX -> Res.string.rule_type_regex
        }

internal val CustomRuleType.shortTitle: StringResource
    get() =
        when (this) {
            CustomRuleType.PREFIX -> Res.string.rule_type_prefix_short
            CustomRuleType.EXACT_NUMBER -> Res.string.rule_type_exact_short
            CustomRuleType.HIDDEN_NUMBER -> Res.string.rule_type_hidden_short
            CustomRuleType.REGEX -> Res.string.rule_type_regex_short
        }

internal val CustomRuleType.icon: ImageVector
    get() =
        when (this) {
            CustomRuleType.PREFIX -> KnoblyIcons.Prefix
            CustomRuleType.EXACT_NUMBER -> KnoblyIcons.Phone
            CustomRuleType.HIDDEN_NUMBER -> KnoblyIcons.Hidden
            CustomRuleType.REGEX -> KnoblyIcons.Regex
        }

internal val CustomRuleError.message: StringResource
    get() =
        when (this) {
            CustomRuleError.EMPTY_VALUE -> Res.string.rule_error_empty
            CustomRuleError.INVALID_PREFIX -> Res.string.rule_error_invalid_prefix
            CustomRuleError.INVALID_NUMBER -> Res.string.rule_error_invalid_number
            CustomRuleError.INVALID_REGEX -> Res.string.rule_error_invalid_regex
            CustomRuleError.TOO_LONG -> Res.string.rule_error_too_long
        }

/** The rule's nickname, or its type when it has none. */
@Composable
internal fun CustomRule.displayName(): String = label.ifBlank { stringResource(type.title) }
