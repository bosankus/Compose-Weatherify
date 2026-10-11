package bose.ankush.language.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class FlagRegionTest {
    @Test
    fun regionComesFromTheTag() {
        assertEquals("IN", regionOf("en-IN"))
        assertEquals("IL", regionOf("iw-IL"))
        assertEquals("IN", regionOf("hi_in"))
    }

    @Test
    fun scriptSubtagIsSkipped() {
        assertEquals("CN", regionOf("zh-Hans-CN"))
    }

    @Test
    fun noRegionIsEmpty() {
        assertEquals("", regionOf("en"))
        assertEquals("", regionOf(""))
    }
}
