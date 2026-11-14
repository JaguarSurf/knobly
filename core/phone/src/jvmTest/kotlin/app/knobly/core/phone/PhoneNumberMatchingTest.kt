package app.knobly.core.phone

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** docs/specs/phone-number-matching.md, with the real normalizer (as the app parses contacts and incoming calls). */
class PhoneNumberMatchingTest {
    private val normalizer = LibPhoneNumberNormalizer()

    private fun same(
        incoming: String,
        saved: String,
    ) = PhoneNumberMatching.sameNumber(
        requireNotNull(normalizer.normalize(incoming)),
        requireNotNull(normalizer.normalize(saved)),
    )

    private val incoming = "+55 35 98123-4567"

    @Test
    fun `the same number in different formats`() {
        assertTrue(same(incoming, "+55 (35) 98123-4567"))
        assertTrue(same(incoming, "(35) 98123-4567"))
        assertTrue(same(incoming, "035 98123-4567"))
        assertTrue(same(incoming, "0 15 35 98123-4567"))
        assertTrue(same("035981234567", "35981234567"))
    }

    @Test
    fun `mobile numbers saved without the 9th digit`() {
        assertTrue(same(incoming, "(35) 8123-4567"))
        assertTrue(same(incoming, "035 8123-4567"))
        assertTrue(same(incoming, "+55 35 8123-4567"))
        // And the other way around: the call arrives without the 9.
        assertTrue(same("(35) 8123-4567", "(35) 98123-4567"))
    }

    @Test
    fun `numbers saved without the DDD or the country code`() {
        assertTrue(same(incoming, "98123-4567"))
        assertTrue(same(incoming, "8123-4567"))
        assertTrue(same("+55 11 98123-4567", "8123-4567"))
    }

    @Test
    fun `a different DDD is a different number`() {
        assertFalse(same(incoming, "(11) 98123-4567"))
        assertFalse(same(incoming, "(11) 8123-4567"))
    }

    @Test
    fun `a different subscriber number is a different number`() {
        assertFalse(same(incoming, "(35) 98123-4568"))
        assertFalse(same(incoming, "8123-4568"))
    }

    @Test
    fun `landlines never had a 9th digit`() {
        assertTrue(same("(35) 3123-4567", "3123-4567"))
        assertFalse(same("(35) 3123-4567", "(35) 93123-4567"))
        assertFalse(same("(35) 93123-4567", "3123-4567"))
    }

    @Test
    fun `short numbers must be equal`() {
        assertTrue(same("190", "190"))
        assertFalse(same("4567", "+55 35 98123-4567"))
        assertFalse(same("1234567", "+55 35 91234-5670"))
    }

    @Test
    fun `numbers from other countries must be equal`() {
        assertTrue(same("+1 650-253-0000", "+16502530000"))
        assertFalse(same("+1 650-253-0000", "253-0000"))
        assertFalse(same("+1 650 8123 4567", "8123-4567"))
    }

    @Test
    fun `shared-cost numbers`() {
        assertTrue(same("0303 123 4567", "+55 303 123 4567"))
        assertFalse(same("0303 123 4567", "0304 123 4567"))
    }
}
