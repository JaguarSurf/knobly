package app.knobly.core.designsystem

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [arrangeChips] at density 1 (pixels = dp): the history filters' widths with the least padding are
 * Todas 51, Atendidas 76, Perdidas 68 and Rejeitadas 78 (text + 2 × 6), and the normal padding adds 16 to each.
 */
class KnoblyChipRowTest {
    private val filters = listOf(51, 76, 68, 78)

    private fun arrange(maxWidth: Int) = arrangeChips(filters, maxWidth, spacing = 8, minSpacing = 6, extraPadding = 16)

    @Test
    fun `keeps the normal padding when the chips fit`() {
        // 412 dp phone: 376 dp of content, the chips need 361.
        assertEquals(
            listOf(ChipSlot(0, 0, 67), ChipSlot(0, 75, 92), ChipSlot(0, 175, 84), ChipSlot(0, 267, 94)),
            arrange(maxWidth = 376),
        )
    }

    @Test
    fun `shrinks the padding and shares the whole width on a 360 dp phone`() {
        // 360 dp phone: 324 dp of content; 273 + 3 × 6 = 291 leaves 33, about 8 more for each chip.
        val slots = arrange(maxWidth = 324)
        assertEquals(listOf(0, 0, 0, 0), slots.map { it.line })
        assertEquals(listOf(60, 84, 76, 86), slots.map { it.width })
        assertEquals(324, slots.last().x + slots.last().width)
    }

    @Test
    fun `wraps whole chips only when even the least padding does not fit`() {
        val slots = arrange(maxWidth = 280)
        assertEquals(listOf(0, 0, 0, 1), slots.map { it.line })
        assertEquals(listOf(67, 92, 84, 94), slots.map { it.width })
        assertEquals(0, slots.last().x)
    }

    @Test
    fun `a chip wider than the row gets the whole row`() {
        assertEquals(listOf(ChipSlot(0, 0, 50)), arrangeChips(listOf(80), 50, 8, 6, 16))
    }

    @Test
    fun `an empty row has no slots`() {
        assertEquals(emptyList(), arrangeChips(emptyList(), 324, 8, 6, 16))
    }
}
