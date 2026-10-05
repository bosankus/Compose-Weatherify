package bose.ankush.home.presentation.wander

import bose.ankush.home.domain.model.AirQuality
import kotlin.math.roundToInt

/** OpenWeather AQI band, 1 (good) to 5 (very poor). */
internal enum class AirLevel { GOOD, FAIR, MODERATE, POOR, VERY_POOR }

/** WHO 2021 24 h guideline, µg/m³. */
private const val PM25_LIMIT_UG_M3 = 15.0
private const val PM10_LIMIT_UG_M3 = 45.0
private const val CO_LIMIT_UG_M3 = 4000.0

/** WHO 2021 8 h peak guideline, µg/m³. */
private const val O3_LIMIT_UG_M3 = 100.0
private const val NO2_LIMIT_UG_M3 = 25.0
private const val SO2_LIMIT_UG_M3 = 40.0

/**
 * Reference limits in µg/m³. Approximate WHO 2021 air quality guideline values: 24 h for
 * PM2.5, PM10, NO2, SO2 and CO; 8 h peak for O3. They only scale the tile bars.
 */
internal enum class Pollutant(
    val label: String,
    val limit: Double,
) {
    PM25("PM₂.₅", PM25_LIMIT_UG_M3),
    PM10("PM₁₀", PM10_LIMIT_UG_M3),
    CO("CO", CO_LIMIT_UG_M3),
    O3("O₃", O3_LIMIT_UG_M3),
    NO2("NO₂", NO2_LIMIT_UG_M3),
    SO2("SO₂", SO2_LIMIT_UG_M3),
}

internal data class PollutantReading(
    val pollutant: Pollutant,
    val value: Double,
) {
    /** Value as a share of its reference limit. 1.0 is at the limit. */
    val share: Double get() = if (value > 0) value / pollutant.limit else 0.0

    /** Bar fill, 0..1. */
    val fraction: Float get() = share.coerceIn(0.0, 1.0).toFloat()

    val level: AirLevel get() = levelForShare(share)
}

/** Band from the AQI index. Out-of-range values clamp to the nearest band. */
internal fun airLevelFor(aqi: Int): AirLevel = AirLevel.entries[(aqi - 1).coerceIn(0, AirLevel.entries.lastIndex)]

/** Bar color band from the share of the reference limit. */
internal fun levelForShare(share: Double): AirLevel =
    when {
        share <= GOOD_SHARE -> AirLevel.GOOD
        share <= FAIR_SHARE -> AirLevel.FAIR
        share <= MODERATE_SHARE -> AirLevel.MODERATE
        share <= POOR_SHARE -> AirLevel.POOR
        else -> AirLevel.VERY_POOR
    }

/** Tile order: PM2.5, PM10, CO on the first row, O3, NO2, SO2 on the second. */
internal fun AirQuality.pollutantReadings(): List<PollutantReading> =
    listOf(
        PollutantReading(Pollutant.PM25, pm25),
        PollutantReading(Pollutant.PM10, pm10),
        PollutantReading(Pollutant.CO, co),
        PollutantReading(Pollutant.O3, o3),
        PollutantReading(Pollutant.NO2, no2),
        PollutantReading(Pollutant.SO2, so2),
    )

/** The reading with the highest share of its limit, or null when nothing was measured. */
internal fun List<PollutantReading>.dominant(): PollutantReading? = filter { it.value > 0 }.maxByOrNull { it.share }

/** AQI index as a plain number, never zero-padded. */
internal fun formatAqi(aqi: Int): String = aqi.toString()

/** Whole numbers from 10 up, one decimal below that so small readings don't read as 0. */
internal fun formatConcentration(value: Double): String =
    if (value >= DECIMAL_BELOW) {
        value.roundToInt().toString()
    } else {
        ((value * TENTHS_FACTOR).roundToInt() / TENTHS_FACTOR).toString().removeSuffix(".0")
    }

private const val GOOD_SHARE = 0.5
private const val FAIR_SHARE = 1.0
private const val MODERATE_SHARE = 1.5
private const val POOR_SHARE = 2.0
private const val DECIMAL_BELOW = 10.0
private const val TENTHS_FACTOR = 10.0
