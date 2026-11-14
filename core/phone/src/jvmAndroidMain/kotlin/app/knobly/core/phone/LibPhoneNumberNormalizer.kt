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

    override fun format(number: PhoneNumber): String {
        val parsed =
            number.e164?.let {
                try {
                    util.parse(it, defaultRegion)
                } catch (_: NumberParseException) {
                    null
                }
            } ?: return formatNonGeographic(number.raw) ?: number.raw
        val style =
            if (util.getRegionCodeForNumber(parsed) == defaultRegion) {
                PhoneNumberUtil.PhoneNumberFormat.NATIONAL
            } else {
                PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL
            }
        return util.format(parsed, style)
    }

    /**
     * Brazilian non-geographic numbers that libphonenumber does not know as valid (e.g. `0304` donations) are still
     * shown the way they are written in Brazil: `0304 765 4321`.
     */
    private fun formatNonGeographic(raw: String): String? =
        brazilianNonGeographic
            .takeIf { defaultRegion == "BR" }
            ?.matchEntire(raw)
            ?.destructured
            ?.let { (service, block, line) -> "$service $block $line" }

    private companion object {
        val brazilianNonGeographic = Regex("^(0[3589]0\\d)(\\d{3})(\\d{4})$")
    }
}
