package bose.ankush.home.presentation.screen

import bose.ankush.home.domain.model.WeatherForecast
import java.util.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class AlertRowContentTest {
    private lateinit var originalZone: TimeZone

    @BeforeTest
    fun pinZone() {
        originalZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @AfterTest
    fun restoreZone() {
        TimeZone.setDefault(originalZone)
    }

    @Test
    fun singleAlertRendersOneRowWithEventNameAndFormattedStart() {
        val alert = alert(event = "Heat advisory", start = OCT_9_0853_UTC)

        val rows = listOf(alert).toAlertRows()

        assertEquals(1, rows.size)
        assertEquals("Heat advisory", rows.single().title)
        assertEquals("Oct 9, 8:53 AM", rows.single().startText)
    }

    @Test
    fun multipleAlertsRenderOneRowEachInOrder() {
        val first = alert(event = "Heat advisory", start = OCT_9_0853_UTC)
        val second = alert(event = "Flood watch", start = OCT_9_1053_UTC)
        val third = alert(event = "Thunderstorm warning", start = OCT_9_1300_UTC)

        val rows = listOf(first, second, third).toAlertRows()

        assertEquals(listOf("Heat advisory", "Flood watch", "Thunderstorm warning"), rows.map { it.title })
        assertEquals(listOf("Oct 9, 8:53 AM", "Oct 9, 10:53 AM", "Oct 9, 1:00 PM"), rows.map { it.startText })
    }

    @Test
    fun missingEventNameFallsBackToWeatherAlertNotDescription() {
        val row =
            listOf(alert(event = null, description = "Strong winds expected"))
                .toAlertRows()
                .single()

        assertEquals("Weather alert", row.title)
    }

    @Test
    fun blankEventNameFallsBackToWeatherAlertNotDescription() {
        val row =
            listOf(alert(event = "   ", description = "Strong winds expected"))
                .toAlertRows()
                .single()

        assertEquals("Weather alert", row.title)
    }

    @Test
    fun missingStartTimeLeavesStartTextEmpty() {
        val row = listOf(alert(event = "Fog", start = null)).toAlertRows().single()

        assertNull(row.startText)
    }

    @Test
    fun nullAndEmptyAlertsDoNotRenderRows() {
        val kept = alert(event = "Fog", start = OCT_9_0853_UTC)

        val rows = listOf(null, alert(event = " ", description = ""), kept).toAlertRows()

        assertEquals(1, rows.size)
        assertSame(kept, rows.single().alert)
    }

    @Test
    fun eachRowCarriesTheAlertPassedToOnOpenAlert() {
        val alerts = listOf(alert(event = "Heat advisory"), alert(event = "Flood watch"))

        val rows = alerts.toAlertRows()

        alerts.forEachIndexed { index, alert -> assertSame(alert, rows[index].alert) }
    }

    private fun alert(
        event: String?,
        description: String? = null,
        start: Long? = null,
    ) = WeatherForecast.Alert(
        description = description,
        end = null,
        event = event,
        sender_name = null,
        start = start,
    )

    private companion object {
        const val OCT_9_0853_UTC = 1_760_000_000L
        const val OCT_9_1053_UTC = 1_760_007_200L
        const val OCT_9_1300_UTC = 1_760_014_800L
    }
}
