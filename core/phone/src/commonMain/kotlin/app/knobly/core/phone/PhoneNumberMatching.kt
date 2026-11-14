package app.knobly.core.phone

import app.knobly.core.model.PhoneNumber

/**
 * Whether two numbers are the same line, the way people store numbers in Brazil: with or without the country code
 * (`+55`), the DDD area code and the 9th mobile digit. Rules and examples: docs/specs/phone-number-matching.md.
 *
 * Brazilian numbers (`+55`, or written without a country code) are compared by their national digits:
 * - the last [SUBSCRIBER_DIGITS] digits must be equal;
 * - the DDD must be equal only when both numbers have one;
 * - the 9th digit may be missing on either side when the number starts with 6, 7, 8 or 9 (the mobile numbers that
 *   got the extra 9 between 2012 and 2016). Landlines (2 to 5) never got it, so there it must match exactly.
 *
 * Numbers with fewer than [SUBSCRIBER_DIGITS] digits (short codes such as `190`) and numbers from other countries
 * must be equal.
 */
object PhoneNumberMatching {
    /** A Brazilian number without the DDD and the 9th digit: `8123-4567`. */
    const val SUBSCRIBER_DIGITS = 8

    private const val BRAZIL = "+55"
    private const val DDD_DIGITS = 2
    private const val NINTH_DIGIT = '9'

    /** First digits of the 8-digit mobile numbers that became 9-digit numbers. */
    private const val OLD_MOBILE_FIRST_DIGITS = "6789"

    fun sameNumber(
        a: PhoneNumber,
        b: PhoneNumber,
    ): Boolean =
        when {
            a.e164 != null && a.e164 == b.e164 -> true
            a.isBrazilian() && b.isBrazilian() -> sameBrazilianNumber(a.national, b.national)
            else -> a.e164 == null && b.e164 == null && a.raw == b.raw
        }

    /** The index key of a number: its last [SUBSCRIBER_DIGITS] digits (or all of them, when shorter). */
    fun indexKey(digits: String): String = digits.takeLast(SUBSCRIBER_DIGITS)

    private fun PhoneNumber.isBrazilian(): Boolean =
        e164?.startsWith(BRAZIL) ?: (!raw.startsWith("+") || raw.startsWith(BRAZIL))

    private fun sameBrazilianNumber(
        x: String,
        y: String,
    ): Boolean {
        if (x == y) return true
        val subscriber = x.takeLast(SUBSCRIBER_DIGITS)
        val headX = head(x)
        val headY = head(y)
        return when {
            headX == null || headY == null -> false
            subscriber != y.takeLast(SUBSCRIBER_DIGITS) -> false
            headX.ddd != null && headY.ddd != null && headX.ddd != headY.ddd -> false
            else -> headX.ninthDigit == headY.ninthDigit || subscriber.first() in OLD_MOBILE_FIRST_DIGITS
        }
    }

    /** What comes before the last 8 digits: nothing, `9`, a DDD, or a DDD + `9`. Anything else: `null`. */
    private fun head(national: String): Head? {
        if (national.length < SUBSCRIBER_DIGITS) return null
        val digits = national.dropLast(SUBSCRIBER_DIGITS)
        return when {
            digits.isEmpty() -> Head(ddd = null, ninthDigit = false)
            digits == NINTH_DIGIT.toString() -> Head(ddd = null, ninthDigit = true)
            digits.length == DDD_DIGITS -> Head(ddd = digits, ninthDigit = false)
            digits.length == DDD_DIGITS + 1 && digits.last() == NINTH_DIGIT -> Head(digits.take(DDD_DIGITS), true)
            else -> null
        }
    }

    private data class Head(
        val ddd: String?,
        val ninthDigit: Boolean,
    )
}
