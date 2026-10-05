package bose.ankush.home.presentation.wander

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WanderAnimatedValueTest {
    @Test
    fun formatWanderNumberUsesPlaceholderForNull() {
        assertEquals("--", formatWanderNumber(null))
        assertEquals("--", formatWanderNumber(Double.NaN, suffix = "°"))
        assertEquals("21°", formatWanderNumber(21.0, suffix = "°"))
        assertEquals("3.5 m/s", formatWanderNumber(3.5, decimals = 1, suffix = " m/s"))
        assertEquals("40%", formatWanderNumber(40.0, suffix = "%"))
    }

    @Test
    fun parseAndApplyKeepUnits() {
        assertEquals(21.0, parseWanderNumber("21°"))
        assertEquals(3.5, parseWanderNumber("3.5 m/s"))
        assertNull(parseWanderNumber("--"))
        assertTrue(isWanderPlaceholder("--"))
        assertTrue(isWanderPlaceholder("—"))
        assertFalse(isWanderPlaceholder("16°"))
        assertEquals("22°", applyWanderNumber("21°", "22"))
        assertEquals("4 m/s", applyWanderNumber("3.5 m/s", "4"))
    }

    @Test
    fun placeholderContentUsesDashes() {
        val content = placeholderWanderContent()
        assertEquals("--", content.temperature)
        assertEquals("--", content.feel)
        assertEquals("--", content.wind)
        assertEquals("--", content.uv)
        assertTrue(content.days.isEmpty())
    }
}
