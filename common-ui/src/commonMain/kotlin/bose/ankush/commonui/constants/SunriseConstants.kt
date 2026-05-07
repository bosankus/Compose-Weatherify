package bose.ankush.commonui.constants

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Configuration constants for the sunrise/sunset animation system.
 * Contains timing, visual, and behavioral parameters organized into logical groups.
 */
object SunriseConstants {
    /** Animation timing constants in milliseconds. */
    object Durations {
        const val INITIAL_ANIMATION = 3000
        const val STAR_TWINKLE = 2000
        const val ATMOSPHERIC_GLOW = 4000
        const val CLOUD_DRIFT = 3000
    }

    /** Size and layout dimension constants for visual elements. */
    object Dimensions {
        val CORNER_RADIUS = 12.dp
        const val MOON_BASE_RADIUS = 12f
        const val MOON_RADIUS_VARIATION = 3f
        const val SUN_BASE_RADIUS = 15f
        const val SUN_RADIUS_VARIATION = 5f
        const val STAR_BASE_SIZE = 2f
        const val STAR_SIZE_VARIATION = 1f
        const val SUN_RAY_LENGTH = 25f
        const val SUN_RAY_WIDTH = 2f
        const val CLOUD_WIDTH = 40f
        const val CLOUD_PUFF_RADIUS = 8f
    }

    /** Alpha transparency values for visual elements (0.0-1.0). */
    object Opacity {
        const val MOON_BASE = 0.8f
        const val MOON_VARIATION = 0.2f
        const val SUN_BASE = 0.9f
        const val SUN_VARIATION = 0.1f
        const val STAR_BASE_BEFORE_SUNRISE = 0.8f
        const val STAR_BASE_AFTER_SUNSET = 0.6f
        const val TWINKLE_VARIATION = 0.3f
        const val TWINKLE_BASE = 0.7f
        const val CLOUD_BASE = 0.6f
        const val CLOUD_VARIATION = 0.2f
    }

    /** Quantity constants for animated elements. */
    object Counts {
        const val SUN_RAY_COUNT = 8
        const val CLOUD_COUNT = 6
        const val CLOUD_PUFFS_PER_CLOUD = 3
    }

    /** Color schemes for different time periods and visual elements. */
    object Colors {
        // Night colors - Deep blue to dark blue-black
        val NIGHT_GRADIENT =
            listOf(
                Color(0xFF000011).copy(alpha = 0.9f), // Almost black with slight blue tint
                Color(0xFF0A1035).copy(alpha = 0.8f), // Very dark blue
                Color(0xFF0F1A4A).copy(alpha = 0.7f), // Dark blue
                Color(0xFF162554).copy(alpha = 0.6f), // Medium-dark blue
            )

        // Dawn colors - Dark blue to purple, pink, orange, yellow
        val DAWN_GRADIENT =
            listOf(
                Color(0xFF0A1035).copy(alpha = 0.8f), // Very dark blue
                Color(0xFF341C5D).copy(alpha = 0.7f), // Deep purple
                Color(0xFF9A3A6A).copy(alpha = 0.6f), // Pink-purple
                Color(0xFFE67E45).copy(alpha = 0.5f), // Orange
            )

        // Day colors - Deep blue to lighter blue
        val DAY_GRADIENT =
            listOf(
                Color(0xFF0E4C92).copy(alpha = 0.7f), // Deep blue
                Color(0xFF1A75FF).copy(alpha = 0.6f), // Medium blue
                Color(0xFF5D9EFF).copy(alpha = 0.5f), // Light blue
                Color(0xFF87CEEB).copy(alpha = 0.4f), // Sky blue
            )

        // Dusk colors - Dark blue to purple, pink, orange, red
        val DUSK_GRADIENT =
            listOf(
                Color(0xFF0A1035).copy(alpha = 0.8f), // Very dark blue
                Color(0xFF341C5D).copy(alpha = 0.7f), // Deep purple
                Color(0xFF9A3A6A).copy(alpha = 0.6f), // Pink-purple
                Color(0xFFE05038).copy(alpha = 0.5f), // Orange-red
            )

        // Default fallback colors - Realistic daytime sky
        val DEFAULT_GRADIENT =
            listOf(
                Color(0xFF0E4C92).copy(alpha = 0.7f), // Deep blue
                Color(0xFF1A75FF).copy(alpha = 0.6f), // Medium blue
                Color(0xFF5D9EFF).copy(alpha = 0.5f), // Light blue
                Color(0xFF87CEEB).copy(alpha = 0.4f), // Sky blue
            )

        // Celestial body colors
        val MOON_COLOR = Color(0xFFF5F5DC)
        val MOON_PHASE_COLOR = Color(0xFF0F0F23)
        val STAR_COLOR = Color.White

        // Cloud colors - Adjusted to match realistic sky gradients
        val CLOUD_DAY_COLOR = Color(0xFFFFFFFF) // Pure white for daytime
        val CLOUD_DAWN_COLOR = Color(0xFFFAE3C6) // Warm cream/peach for sunrise
        val CLOUD_DUSK_COLOR = Color(0xFFFFB8A0) // Soft orange-pink for sunset

        // Sun colors by time - Enhanced for realistic appearance
        val SUN_EARLY_MORNING = Color(0xFFFF7E45) // Warm orange-red for early morning
        val SUN_MORNING = Color(0xFFFFAA33) // Golden orange for morning
        val SUN_MIDDAY = Color(0xFFFFD700) // Bright gold for midday
        val SUN_EVENING = Color(0xFFFFAA33) // Golden orange for evening
        val SUN_LATE_EVENING = Color(0xFFFF7E45) // Warm orange-red for late evening
    }

    /** Spatial positioning and movement parameters (normalized 0.0-1.0). */
    object Positioning {
        const val MOON_BASE_Y = 0.25f
        const val MOON_Y_VARIATION = 0.3f
        const val MOON_Y_AMPLITUDE = 0.1f
        const val MOON_START_X = 0.9f
        const val MOON_END_X = 0.1f
        const val MOON_TRAVEL_DISTANCE = 0.8f

        const val SUN_START_X = 0.1f
        const val SUN_TRAVEL_DISTANCE = 0.8f
        const val SUN_BASE_Y = 0.6f
        const val SUN_Y_AMPLITUDE = 0.4f

        const val CLOUD_BASE_Y = 0.2f
        const val CLOUD_Y_VARIATION = 0.15f
        const val CLOUD_SPACING_X = 0.25f
        const val CLOUD_DRIFT_SPEED = 0.08f
    }

    /** Time-based transition points for animation phases (normalized 0.0-1.0). */
    object TimeThresholds {
        const val DAWN_END = 0.2f
        const val DUSK_START = 0.8f
        const val SUN_MORNING_END = 0.1f
        const val SUN_MIDMORNING_END = 0.2f
        const val SUN_EVENING_START = 0.8f
        const val SUN_LATE_EVENING_START = 0.9f
    }

    /** Predefined star positions as normalized (x, y) coordinates. */
    val STAR_POSITIONS =
        listOf(
            Pair(0.15f, 0.2f),
            Pair(0.3f, 0.15f),
            Pair(0.45f, 0.25f),
            Pair(0.6f, 0.1f),
            Pair(0.75f, 0.3f),
            Pair(0.85f, 0.18f),
            Pair(0.2f, 0.4f),
            Pair(0.4f, 0.45f),
            Pair(0.65f, 0.35f),
            Pair(0.8f, 0.5f),
            Pair(0.1f, 0.6f),
            Pair(0.9f, 0.65f),
        )
}
