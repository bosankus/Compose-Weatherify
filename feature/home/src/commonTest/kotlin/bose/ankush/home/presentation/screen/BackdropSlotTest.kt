package bose.ankush.home.presentation.screen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class BackdropSlotTest {
    @Test
    fun hoursMapToTheirPartOfTheDay() {
        val expected =
            mapOf(
                0 to DayPeriod.MIDNIGHT,
                4 to DayPeriod.MIDNIGHT,
                5 to DayPeriod.DAY,
                11 to DayPeriod.DAY,
                12 to DayPeriod.AFTERNOON,
                16 to DayPeriod.AFTERNOON,
                17 to DayPeriod.EVENING,
                19 to DayPeriod.EVENING,
                20 to DayPeriod.NIGHT,
                23 to DayPeriod.NIGHT,
            )
        expected.forEach { (hour, period) ->
            assertEquals(
                period,
                DayPeriod.ofHour(hour),
                "hour $hour"
            )
        }
    }

    @Test
    fun slotUsesTheForecastPlacesClockNotTheDevices() {
        // 2026-10-11 12:30 UTC is 18:00 in Kolkata (+5:30): evening there, a new day not yet.
        val now = Instant.parse("2026-10-11T12:30:00Z")
        val kolkata = backdropSlot(SkyCondition.CLEAR, "Clear", timezoneOffset = 19_800, now = now)
        val honolulu =
            backdropSlot(SkyCondition.CLEAR, "Clear", timezoneOffset = -36_000, now = now)

        assertEquals(DayPeriod.EVENING, kolkata.period)
        assertEquals(DayPeriod.MIDNIGHT, honolulu.period)
        assertEquals("clear sky landscape sunset", kolkata.searchQuery)
        assertEquals("clear.evening", kolkata.key)
    }

    @Test
    fun pickChangesDailyAndHoldsWithinADay() {
        val results = (0 until BackdropSlot.RESULTS_PER_SEARCH).toList()
        val morning = Instant.parse("2026-10-11T07:00:00Z")
        val laterThatMorning = Instant.parse("2026-10-11T10:59:00Z")
        val nextMorning = Instant.parse("2026-10-12T07:00:00Z")

        val today = backdropSlot(SkyCondition.RAIN, "Rain", 0, morning)
        assertEquals(
            today.pick(results),
            backdropSlot(SkyCondition.RAIN, "Rain", 0, laterThatMorning).pick(results)
        )
        assertNotEquals(
            today.pick(results),
            backdropSlot(SkyCondition.RAIN, "Rain", 0, nextMorning).pick(results)
        )
        assertNotEquals(
            today.cacheKey,
            backdropSlot(SkyCondition.RAIN, "Rain", 0, nextMorning).cacheKey
        )
    }

    @Test
    fun noForecastMeansNoSearch() {
        val slot =
            backdropSlot(SkyCondition.CLOUDS, null, null, Instant.parse("2026-10-11T07:00:00Z"))

        assertNull(slot.searchQuery)
        assertNull(slot.cacheKey)
        assertNull(slot.pick(emptyList<Int>()))
    }
}
