package app.knobly.domain.rules.call

import app.knobly.core.phone.PhoneNumberNormalizer

/** Why a custom rule's value cannot be saved. */
enum class CustomRuleError {
    EMPTY_VALUE,
    INVALID_PREFIX,
    INVALID_NUMBER,
    INVALID_REGEX,
    TOO_LONG,
}

/** Validation and normalization of custom rule values, shared by the rule editor and [buildCustomRule]. */
object CustomRuleValidator {
    /** Keeps regex matching fast inside the call screening service. */
    const val MAX_REGEX_LENGTH = 100

    /** Longest possible phone number (E.164). */
    const val MAX_PREFIX_DIGITS = 15

    private const val MIN_NUMBER_DIGITS = 3
    private val allowedPrefixChars = Regex("^\\+?[0-9 ()\\-.]+$")

    fun validate(
        type: CustomRuleType,
        value: String,
        normalizer: PhoneNumberNormalizer,
    ): CustomRuleError? {
        val trimmed = value.trim()
        return when {
            type == CustomRuleType.HIDDEN_NUMBER -> null
            trimmed.isEmpty() -> CustomRuleError.EMPTY_VALUE
            type == CustomRuleType.PREFIX -> validatePrefix(trimmed)
            type == CustomRuleType.EXACT_NUMBER -> validateNumber(trimmed, normalizer)
            else -> validateRegex(trimmed)
        }
    }

    private fun validatePrefix(value: String): CustomRuleError? =
        CustomRuleError.INVALID_PREFIX.takeIf {
            !allowedPrefixChars.matches(value) || value.count(Char::isDigit) !in 1..MAX_PREFIX_DIGITS
        }

    private fun validateNumber(
        value: String,
        normalizer: PhoneNumberNormalizer,
    ): CustomRuleError? {
        val number = normalizer.normalize(value)
        return CustomRuleError.INVALID_NUMBER.takeIf { number == null || number.national.length < MIN_NUMBER_DIGITS }
    }

    private fun validateRegex(value: String): CustomRuleError? =
        when {
            value.length > MAX_REGEX_LENGTH -> CustomRuleError.TOO_LONG
            runCatching { Regex(value) }.isFailure -> CustomRuleError.INVALID_REGEX
            else -> null
        }

    /** `"0303 123"` -> `"0303123"`, `"+1 (650)"` -> `"+1650"`. */
    fun normalizedPrefix(value: String): String {
        val trimmed = value.trim()
        val digits = trimmed.filter(Char::isDigit)
        return if (trimmed.startsWith("+")) "+$digits" else digits
    }
}
