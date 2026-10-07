package bose.ankush.home.presentation.screen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HeaderLuminanceTest {
    @Test
    fun headerSampleHeightIsTopFortyFivePercent() {
        assertEquals(0, headerSampleHeight(0))
        assertEquals(1, headerSampleHeight(1))
        assertEquals(4, headerSampleHeight(10))
        assertEquals(10, headerSampleHeight(24))
    }

    @Test
    fun headerLuminancePicksInkOnBrightTopEvenWhenFullAverageIsDark() {
        // Bright fog top (~0.9) vs dark full-frame average (~0.35): header text must use ink.
        assertEquals(ContentOnLight, contentColorForLuminance(0.90))
        assertEquals(ContentOnDark, contentColorForLuminance(0.35))
    }

    @Test
    fun conditionHeaderLuminanceUsesTopGradientEnd() {
        for (condition in SkyCondition.entries) {
            val header = condition.dominantHeaderLuminance()
            val average = condition.dominantBackgroundLuminance()
            assertTrue(header in 0.0..1.0)
            // Top ends are lighter than the mean for every current gradient.
            assertTrue(header >= average - 0.001, "$condition header=$header average=$average")
        }
    }
}
