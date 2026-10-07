package bose.ankush.home.presentation.screen

import kotlin.test.Test
import kotlin.test.assertEquals

class WeatherDetailsGridTest {
    @Test
    fun fullRowsAreNotPadded() {
        val rows = detailGridRows(listOf(1, 2, 3, 4, 5, 6), columns = 3)

        assertEquals(listOf(listOf<Int?>(1, 2, 3), listOf<Int?>(4, 5, 6)), rows)
    }

    @Test
    fun singleExtraCellPadsToThreeSlots() {
        val rows = detailGridRows(listOf(1, 2, 3, 4, 5, 6, 7), columns = 3)

        assertEquals(listOf<Int?>(7, null, null), rows.last())
    }

    @Test
    fun twoExtraCellsFillLeftToRightAndLeaveLastSlotEmpty() {
        val rows = detailGridRows(listOf(1, 2, 3, 4, 5, 6, 7, 8), columns = 3)

        assertEquals(3, rows.size)
        assertEquals(listOf<Int?>(7, 8, null), rows.last())
        rows.forEach { assertEquals(3, it.size) }
    }

    @Test
    fun noCellsGivesNoRows() {
        assertEquals(emptyList(), detailGridRows(emptyList<Int>(), columns = 3))
    }
}
