package bose.ankush.home.presentation.screen

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MetricChipColorsTest {
    @Test
    fun darkAveragePicksSoftOffWhiteContent() {
        val colors = metricChipColors(Color(0xFF1A2430))
        assertEquals(ContentOnDark, colors.content)
        assertTrue(colors.surface.alpha in 0.55f..0.65f)
    }

    @Test
    fun lightAveragePicksMutedInkContent() {
        val colors = metricChipColors(Color(0xFFD8DCE2))
        assertEquals(ContentOnLight, colors.content)
        assertTrue(colors.surface.alpha in 0.55f..0.65f)
    }

    @Test
    fun contrastAgainstCompositedSurfaceMeetsWcagAa() {
        for (average in listOf(Color(0xFF0B1218), Color(0xFF8B949C), Color(0xFFE8ECF0), Color(0xFF3A3358))) {
            val colors = metricChipColors(average)
            val composited = compositeOver(colors.surface, average.copy(alpha = 1f))
            assertTrue(
                contrastRatio(colors.content, composited) >= 4.5,
                "contrast for $average was ${contrastRatio(colors.content, composited)}",
            )
        }
    }

    @Test
    fun darkenAndLightenStayOffPureBlackAndWhite() {
        val darkened = darken(Color.White, 0.5f)
        val lightened = lighten(Color.Black, 0.5f)
        assertTrue(darkened.red in 0.05f..0.95f)
        assertTrue(lightened.red in 0.05f..0.95f)
    }

    @Test
    fun conditionGradientFallbackProducesChipColors() {
        for (condition in SkyCondition.entries) {
            val colors = metricChipColors(condition.dominantBackgroundColor())
            assertTrue(colors.surface.alpha in 0.55f..0.65f)
            assertTrue(colors.content == ContentOnDark || colors.content == ContentOnLight)
        }
    }
}
