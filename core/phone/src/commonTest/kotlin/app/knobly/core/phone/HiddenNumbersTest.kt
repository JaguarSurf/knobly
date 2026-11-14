package app.knobly.core.phone

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HiddenNumbersTest {
    @Test
    fun `missing or blank caller ids are hidden`() {
        assertTrue(HiddenNumbers.isHidden(null))
        assertTrue(HiddenNumbers.isHidden(""))
        assertTrue(HiddenNumbers.isHidden("   "))
    }

    @Test
    fun `withheld markers are hidden`() {
        listOf("anonymous", "Private", "RESTRICTED", "unknown", "-1", "-2").forEach {
            assertTrue(HiddenNumbers.isHidden(it), "expected '$it' to be hidden")
        }
    }

    @Test
    fun `real numbers are not hidden`() {
        assertFalse(HiddenNumbers.isHidden("+5511912345678"))
        assertFalse(HiddenNumbers.isHidden("0303 123 4567"))
    }
}
