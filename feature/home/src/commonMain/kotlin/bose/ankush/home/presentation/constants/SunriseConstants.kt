package bose.ankush.home.presentation.constants

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import bose.ankush.commonui.theme.SunriseCloudDawnColor
import bose.ankush.commonui.theme.SunriseCloudDayColor
import bose.ankush.commonui.theme.SunriseCloudDuskColor
import bose.ankush.commonui.theme.SunriseDawnGradientEnd
import bose.ankush.commonui.theme.SunriseDawnGradientMid1
import bose.ankush.commonui.theme.SunriseDawnGradientMid2
import bose.ankush.commonui.theme.SunriseDawnGradientStart
import bose.ankush.commonui.theme.SunriseDayGradientEnd
import bose.ankush.commonui.theme.SunriseDayGradientMid1
import bose.ankush.commonui.theme.SunriseDayGradientMid2
import bose.ankush.commonui.theme.SunriseDayGradientStart
import bose.ankush.commonui.theme.SunriseDuskGradientEnd
import bose.ankush.commonui.theme.SunriseMoonColor
import bose.ankush.commonui.theme.SunriseMoonPhaseColor
import bose.ankush.commonui.theme.SunriseNightGradientEnd
import bose.ankush.commonui.theme.SunriseNightGradientMid1
import bose.ankush.commonui.theme.SunriseNightGradientMid2
import bose.ankush.commonui.theme.SunriseNightGradientStart
import bose.ankush.commonui.theme.SunriseSunEarlyMorning
import bose.ankush.commonui.theme.SunriseSunMidday
import bose.ankush.commonui.theme.SunriseSunMorning

object SunriseConstants {
    object Durations {
        const val INITIAL_ANIMATION = 3000
        const val STAR_TWINKLE = 2000
        const val ATMOSPHERIC_GLOW = 4000
        const val CLOUD_DRIFT = 3000
    }

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

    object Counts {
        const val SUN_RAY_COUNT = 8
        const val CLOUD_COUNT = 6
        const val CLOUD_PUFFS_PER_CLOUD = 3
    }

    object Colors {
        val NIGHT_GRADIENT =
            listOf(
                SunriseNightGradientStart.copy(alpha = 0.9f),
                SunriseNightGradientMid1.copy(alpha = 0.8f),
                SunriseNightGradientMid2.copy(alpha = 0.7f),
                SunriseNightGradientEnd.copy(alpha = 0.6f),
            )

        val DAWN_GRADIENT =
            listOf(
                SunriseDawnGradientStart.copy(alpha = 0.8f),
                SunriseDawnGradientMid1.copy(alpha = 0.7f),
                SunriseDawnGradientMid2.copy(alpha = 0.6f),
                SunriseDawnGradientEnd.copy(alpha = 0.5f),
            )

        val DAY_GRADIENT =
            listOf(
                SunriseDayGradientStart.copy(alpha = 0.7f),
                SunriseDayGradientMid1.copy(alpha = 0.6f),
                SunriseDayGradientMid2.copy(alpha = 0.5f),
                SunriseDayGradientEnd.copy(alpha = 0.4f),
            )

        val DUSK_GRADIENT =
            listOf(
                SunriseDawnGradientStart.copy(alpha = 0.8f),
                SunriseDawnGradientMid1.copy(alpha = 0.7f),
                SunriseDawnGradientMid2.copy(alpha = 0.6f),
                SunriseDuskGradientEnd.copy(alpha = 0.5f),
            )

        val DEFAULT_GRADIENT =
            listOf(
                SunriseDayGradientStart.copy(alpha = 0.7f),
                SunriseDayGradientMid1.copy(alpha = 0.6f),
                SunriseDayGradientMid2.copy(alpha = 0.5f),
                SunriseDayGradientEnd.copy(alpha = 0.4f),
            )

        val MOON_COLOR = SunriseMoonColor
        val MOON_PHASE_COLOR = SunriseMoonPhaseColor
        val STAR_COLOR = Color.White

        val CLOUD_DAY_COLOR = SunriseCloudDayColor
        val CLOUD_DAWN_COLOR = SunriseCloudDawnColor
        val CLOUD_DUSK_COLOR = SunriseCloudDuskColor

        val SUN_EARLY_MORNING = SunriseSunEarlyMorning
        val SUN_MORNING = SunriseSunMorning
        val SUN_MIDDAY = SunriseSunMidday
        val SUN_EVENING = SunriseSunMorning
        val SUN_LATE_EVENING = SunriseSunEarlyMorning
    }

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

    object TimeThresholds {
        const val DAWN_END = 0.2f
        const val DUSK_START = 0.8f
        const val SUN_MORNING_END = 0.1f
        const val SUN_MIDMORNING_END = 0.2f
        const val SUN_EVENING_START = 0.8f
        const val SUN_LATE_EVENING_START = 0.9f
    }

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
