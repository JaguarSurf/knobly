package app.knobly.core.model

/**
 * A phone number as seen by the rule engine.
 *
 * @property raw the digits as received (a leading `+` is kept), without any formatting.
 * @property e164 the E.164 form (e.g. `+5511912345678`) when the number is a valid number, otherwise `null`.
 *   Note: Brazilian `0300`/`0303` shared-cost numbers are valid (`0303 123 4567` -> `+553031234567`),
 *   while `0304` numbers are not.
 * @property national the national significant number, without trunk (`0`) or carrier prefixes
 *   (e.g. `11912345678`, `3031234567`); the received digits when the number cannot be parsed at all.
 */
data class PhoneNumber(
    val raw: String,
    val e164: String?,
    val national: String,
)
