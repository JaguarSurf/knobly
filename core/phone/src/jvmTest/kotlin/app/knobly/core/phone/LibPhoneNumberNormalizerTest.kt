package app.knobly.core.phone

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LibPhoneNumberNormalizerTest {
    private val normalizer = LibPhoneNumberNormalizer()

    private fun e164(raw: String) = normalizer.normalize(raw)?.e164

    @Test
    fun `international format with +55`() {
        val number = normalizer.normalize("+55 11 91234-5678")
        assertEquals("+5511912345678", number?.e164)
        assertEquals("11912345678", number?.national)
        assertEquals("+5511912345678", number?.raw)
    }

    @Test
    fun `national format with DDD area code`() {
        assertEquals("+5511912345678", e164("(11) 91234-5678"))
        assertEquals("+5521987654321", e164("21 98765 4321"))
    }

    @Test
    fun `trunk prefix 0 before the DDD`() {
        assertEquals("+5511912345678", e164("011 91234-5678"))
    }

    @Test
    fun `carrier selection code before the DDD`() {
        // 0 + carrier code (21) + DDD (11) + number
        assertEquals("+5511912345678", e164("0 21 11 91234-5678"))
    }

    @Test
    fun `mobile numbers have the 9th digit and landlines have 8 digits`() {
        assertEquals("+5511912345678", e164("11 91234-5678"))
        assertEquals("+551134567890", e164("11 3456-7890"))
    }

    @Test
    fun `old 8-digit mobile number without the 9th digit is not a valid number`() {
        val number = normalizer.normalize("11 8123-4567")
        assertNull(number?.e164)
        assertEquals("1181234567", number?.national)
    }

    @Test
    fun `hidden numbers normalize to null`() {
        assertNull(normalizer.normalize(null))
        assertNull(normalizer.normalize(""))
        assertNull(normalizer.normalize("anonymous"))
    }

    @Test
    fun `0303 telemarketing is a valid shared-cost number and loses the trunk prefix`() {
        val telemarketing = normalizer.normalize("0303 123 4567")
        assertEquals("+553031234567", telemarketing?.e164)
        assertEquals("3031234567", telemarketing?.national)
    }

    @Test
    fun `0304 donations is not a valid number but still loses the trunk prefix`() {
        val donations = normalizer.normalize("0304-765-4321")
        assertNull(donations?.e164)
        assertEquals("3047654321", donations?.national)
        assertEquals("03047654321", donations?.raw)
    }

    @Test
    fun `foreign numbers keep their own country code`() {
        assertEquals("+16502530000", e164("+1 650 253 0000"))
    }

    private fun display(raw: String) = normalizer.format(requireNotNull(normalizer.normalize(raw)))

    @Test
    fun `display format is national for Brazilian numbers and international otherwise`() {
        assertEquals("(11) 91234-5678", display("+5511912345678"))
        assertEquals("(11) 4002-8922", display("1140028922"))
        assertEquals("0303 123 4567", display("03031234567"))
        assertEquals("+1 650-253-0000", display("+16502530000"))
    }

    @Test
    fun `Brazilian non-geographic numbers are formatted even when libphonenumber rejects them`() {
        assertEquals("0304 765 4321", display("0304 765 4321"))
    }

    @Test
    fun `other numbers that cannot be parsed are shown as received`() {
        assertEquals("12345", display("12345"))
    }
}
