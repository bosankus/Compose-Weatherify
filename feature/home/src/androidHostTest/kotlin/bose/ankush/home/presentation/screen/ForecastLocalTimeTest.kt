package bose.ankush.home.presentation.screen

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import java.util.TimeZone as JavaTimeZone

class ForecastLocalTimeTest {
    private lateinit var originalZone: JavaTimeZone

    @BeforeTest
    fun pinDeviceToIst() {
        originalZone = JavaTimeZone.getDefault()
        JavaTimeZone.setDefault(JavaTimeZone.getTimeZone("Asia/Kolkata"))
    }

    @AfterTest
    fun restoreZone() {
        JavaTimeZone.setDefault(originalZone)
    }

    @Test
    fun mountainViewSunTimesUseTheForecastOffsetNotTheDevice() {
        val pdt = forecastTimeZone(MOUNTAIN_VIEW_OFFSET)

        assertEquals("7:06 AM", SUNRISE.toClock(pdt))
        assertEquals("6:47 PM", SUNSET.toClock(pdt))
    }

    @Test
    fun deviceZoneShowsTheReportedBugForComparison() {
        assertEquals("7:36 PM", SUNRISE.toClock())
        assertEquals("7:17 AM", SUNSET.toClock())
    }

    @Test
    fun missingOffsetFallsBackToTheDeviceZone() {
        assertEquals("7:36 PM", SUNRISE.toClock(forecastTimeZone(null)))
    }

    @Test
    fun observedLabelUsesTheForecastZoneForDateAndTime() {
        assertEquals("Sun, 04 Oct, 8:22 PM", OBSERVED.let { observedLabel(it, forecastTimeZone(MOUNTAIN_VIEW_OFFSET)) })
        assertEquals("Mon, 05 Oct, 8:52 AM", observedLabel(OBSERVED, forecastTimeZone(null)))
    }

    @Test
    fun forecastZoneUsesTheForecastOffsetAndFallsBackToTheDevice() {
        assertEquals("7:06 AM", SUNRISE.toClock(forecastZone(MOUNTAIN_VIEW_OFFSET)))
        assertEquals("7:36 PM", SUNRISE.toClock(forecastZone(null)))
    }

    @Test
    fun midnightAndNoonUseTwelve() {
        val utc = forecastTimeZone(0)

        assertEquals("12:00 AM", MIDNIGHT_UTC.toClock(utc))
        assertEquals("12:00 PM", (MIDNIGHT_UTC + 12 * 3600).toClock(utc))
    }

    @Test
    fun headerUsesTheDailySummaryFirst() {
        assertEquals(
            "Expect a day of partly cloudy with clear spells",
            headerSummaryLine(
                " Expect a day of partly cloudy with clear spells ",
                "clear sky",
                "It's clear",
            ),
        )
    }

    @Test
    fun headerFallsBackToCapitalisedDescriptionWhenSummaryIsBlank() {
        assertEquals("Clear sky", headerSummaryLine("   ", "clear sky", "It's clear"))
        assertEquals("Clear sky", headerSummaryLine(null, "clear sky", "It's clear"))
    }

    @Test
    fun headerFallsBackToConditionLineWhenBothAreMissing() {
        assertEquals("It's clear", headerSummaryLine(null, "", "It's clear"))
    }

    private companion object {
        const val MOUNTAIN_VIEW_OFFSET = -25200

        // 2026-10-04 14:06 UTC and 2026-10-05 01:47 UTC.
        const val SUNRISE = 1791122760L
        const val SUNSET = 1791164820L

        // 2026-10-05 03:22 UTC.
        const val OBSERVED = 1791170520L

        // 2026-10-04 00:00 UTC.
        const val MIDNIGHT_UTC = 1791072000L
    }
}
