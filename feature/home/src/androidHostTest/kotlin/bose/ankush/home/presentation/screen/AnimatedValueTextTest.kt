package bose.ankush.home.presentation.screen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AnimatedValueTextTest {
    @Test
    fun formatAnimatedNumberUsesPlaceholderForNull() {
        assertEquals("--", formatAnimatedNumber(null))
        assertEquals("--", formatAnimatedNumber(Double.NaN, suffix = "°"))
        assertEquals("21°", formatAnimatedNumber(21.0, suffix = "°"))
        assertEquals("3.5 m/s", formatAnimatedNumber(3.5, decimals = 1, suffix = " m/s"))
        assertEquals("40%", formatAnimatedNumber(40.0, suffix = "%"))
    }

    @Test
    fun parseAndApplyKeepUnits() {
        assertEquals(21.0, parseAnimatedNumber("21°"))
        assertEquals(3.5, parseAnimatedNumber("3.5 m/s"))
        assertNull(parseAnimatedNumber("--"))
        assertTrue(isValuePlaceholder("--"))
        assertTrue(isValuePlaceholder("—"))
        assertFalse(isValuePlaceholder("16°"))
        assertEquals("22°", applyAnimatedNumber("21°", "22"))
        assertEquals("4 m/s", applyAnimatedNumber("3.5 m/s", "4"))
    }

    @Test
    fun placeholderContentUsesDashes() {
        val content = placeholderHomeWeatherContent()
        assertEquals("--", content.temperature)
        assertEquals("--", content.feel)
        assertEquals("--", content.wind)
        assertEquals("--", content.uv)
        assertTrue(content.days.isEmpty())
    }
}
