package app.knobly.core.phone

import app.knobly.core.model.PhoneNumber

/**
 * Minimal [PhoneNumberNormalizer] without number metadata: keeps the digits (and a leading `+`), never produces an
 * E.164 form. Used where libphonenumber is not available (iOS, until PhoneNumberKit is wired from Swift).
 */
class DigitsOnlyPhoneNumberNormalizer : PhoneNumberNormalizer {
    override fun normalize(raw: String?): PhoneNumber? {
        if (HiddenNumbers.isHidden(raw)) return null
        val value = requireNotNull(raw).trim()
        val digits = value.filter(Char::isDigit)
        val withPlus = if (value.startsWith("+")) "+$digits" else digits
        return PhoneNumber(raw = withPlus, e164 = null, national = digits.removePrefix("0"))
    }
}
