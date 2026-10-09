package bose.ankush.home.presentation.screen

import bose.ankush.home.domain.model.AirQuality
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AirQualityLevelsTest {
    @Test
    fun aqiIndexMapsToItsLevelAndClampsOutOfRange() {
        assertEquals(
            listOf(AirLevel.GOOD, AirLevel.FAIR, AirLevel.MODERATE, AirLevel.POOR, AirLevel.VERY_POOR),
            (1..5).map(::airLevelFor),
        )
        assertEquals(AirLevel.GOOD, airLevelFor(0))
        assertEquals(AirLevel.VERY_POOR, airLevelFor(9))
    }

    @Test
    fun barFractionIsValueOverLimitClampedToZeroAndOne() {
        assertEquals(0.5f, PollutantReading(Pollutant.PM25, 7.5).fraction)
        assertEquals(1f, PollutantReading(Pollutant.NO2, 80.0).fraction)
        assertEquals(0f, PollutantReading(Pollutant.SO2, -3.0).fraction)
        assertEquals(0f, PollutantReading(Pollutant.CO, 0.0).fraction)
    }

    @Test
    fun barLevelFollowsShareOfLimit() {
        assertEquals(AirLevel.GOOD, PollutantReading(Pollutant.PM10, 20.0).level)
        assertEquals(AirLevel.FAIR, PollutantReading(Pollutant.PM10, 45.0).level)
        assertEquals(AirLevel.MODERATE, PollutantReading(Pollutant.PM10, 60.0).level)
        assertEquals(AirLevel.POOR, PollutantReading(Pollutant.PM10, 90.0).level)
        assertEquals(AirLevel.VERY_POOR, PollutantReading(Pollutant.PM10, 91.0).level)
    }

    @Test
    fun dominantPollutantIsHighestShareNotHighestValue() {
        // Screenshot values: CO 120 is the largest number but only 3% of its limit; O3 78 is 78%.
        val air = AirQuality(aqi = 2, pm25 = 8.0, pm10 = 18.0, co = 120.0, o3 = 78.0, no2 = 1.0, so2 = 0.0)

        assertEquals(Pollutant.O3, air.pollutantReadings().dominant()?.pollutant)
    }

    @Test
    fun noDominantPollutantWhenNothingWasMeasured() {
        assertNull(AirQuality(aqi = 1).pollutantReadings().dominant())
    }

    @Test
    fun readingsKeepTheGridOrder() {
        assertEquals(
            listOf(Pollutant.PM25, Pollutant.PM10, Pollutant.CO, Pollutant.O3, Pollutant.NO2, Pollutant.SO2),
            AirQuality(aqi = 1).pollutantReadings().map { it.pollutant },
        )
    }

    @Test
    fun aqiNumberIsNotZeroPadded() {
        assertEquals("2", formatAqi(2))
        assertEquals("5", formatAqi(5))
    }

    @Test
    fun concentrationShowsOneDecimalOnlyBelowTen() {
        assertEquals("120", formatConcentration(120.4))
        assertEquals("18", formatConcentration(18.0))
        assertEquals("0.4", formatConcentration(0.43))
        assertEquals("1", formatConcentration(1.0))
        assertEquals("0", formatConcentration(0.0))
    }
}
