package app.knobly.domain.rules.call

import app.knobly.core.phone.LibPhoneNumberNormalizer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CustomRuleValidatorTest {
    private val normalizer = LibPhoneNumberNormalizer()

    private fun validate(
        type: CustomRuleType,
        value: String,
    ) = CustomRuleValidator.validate(type, value, normalizer)

    @Test
    fun prefixes() {
        assertNull(validate(CustomRuleType.PREFIX, "0303"))
        assertNull(validate(CustomRuleType.PREFIX, "+1"))
        assertNull(validate(CustomRuleType.PREFIX, "11 4004-"))
        assertEquals(CustomRuleError.EMPTY_VALUE, validate(CustomRuleType.PREFIX, "  "))
        assertEquals(CustomRuleError.INVALID_PREFIX, validate(CustomRuleType.PREFIX, "abc"))
        assertEquals(CustomRuleError.INVALID_PREFIX, validate(CustomRuleType.PREFIX, "1234567890123456"))
        assertEquals("0303123", CustomRuleValidator.normalizedPrefix(" 0303 123 "))
        assertEquals("+1650", CustomRuleValidator.normalizedPrefix("+1 (650)"))
    }

    @Test
    fun exactNumbers() {
        assertNull(validate(CustomRuleType.EXACT_NUMBER, "(11) 4002-8922"))
        assertEquals(CustomRuleError.EMPTY_VALUE, validate(CustomRuleType.EXACT_NUMBER, ""))
        assertEquals(CustomRuleError.INVALID_NUMBER, validate(CustomRuleType.EXACT_NUMBER, "12"))
    }

    @Test
    fun hiddenNumbersNeedNoValue() {
        assertNull(validate(CustomRuleType.HIDDEN_NUMBER, ""))
    }

    @Test
    fun regularExpressions() {
        assertNull(validate(CustomRuleType.REGEX, "^0800"))
        assertEquals(CustomRuleError.INVALID_REGEX, validate(CustomRuleType.REGEX, "(["))
        assertEquals(
            CustomRuleError.TOO_LONG,
            validate(
                CustomRuleType.REGEX,
                "1".repeat(CustomRuleValidator.MAX_REGEX_LENGTH + 1),
            ),
        )
    }
}
