package app.knobly.core.phone

import app.knobly.core.model.PhoneNumber
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil

/**
 * [PhoneNumberNormalizer] backed by Google's libphonenumber.
 *
 * Numbers without a country code are parsed for [defaultRegion] (Brazil by default), which handles the trunk
 * prefix `0`, carrier selection codes (`0 XX DDD ...`) and the 9th mobile digit.
 */
class LibPhoneNumberNormalizer(
    private val defaultRegion: String = "BR",
) : PhoneNumberNormalizer {
    private val util = PhoneNumberUtil.getInstance()

    override fun normalize(raw: String?): PhoneNumber? {
        if (HiddenNumbers.isHidden(raw)) return null
        val value = requireNotNull(raw)
        val digits =
            buildString {
                if (value.trim().startsWith("+")) append('+')
                value.filter(Char::isDigit).forEach(::append)
            }

        val parsed =
            try {
                util.parse(value, defaultRegion)
            } catch (_: NumberParseException) {
                null
            }

        return PhoneNumber(
            raw = digits,
            e164 = parsed?.takeIf(util::isValidNumber)?.let { util.format(it, PhoneNumberUtil.PhoneNumberFormat.E164) },
            national = parsed?.let(util::getNationalSignificantNumber) ?: digits.removePrefix("+"),
        )
    }
}
