package bose.ankush.home.presentation.wander

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertTrue

class WanderCornerGlowTest {
    @Test
    fun brightHeaderGetsStrongerButStillSoftAlpha() {
        val bright = wanderCornerGlowColor(Color(0xFFD8DCE2), headerLuminance = 0.78)
        val dim = wanderCornerGlowColor(Color(0xFF1A2430), headerLuminance = 0.22)
        // Color stores 8-bit alpha, so 0.08f / 0.16f round-trip a little.
        assertTrue(bright.alpha in 0.07f..0.18f, "bright alpha ${bright.alpha}")
        assertTrue(dim.alpha in 0.07f..0.12f, "dim alpha ${dim.alpha}")
        assertTrue(bright.alpha > dim.alpha)
    }

    @Test
    fun glowStaysBelowSoloRangeBecauseOfTopScrim() {
        val color = wanderCornerGlowColor(Color(0xFFE8ECF0), headerLuminance = 0.9)
        assertTrue(
            color.alpha <= 0.17f,
            "alpha ${color.alpha} should stay under the solo 0.18–0.28 band",
        )
    }
}
