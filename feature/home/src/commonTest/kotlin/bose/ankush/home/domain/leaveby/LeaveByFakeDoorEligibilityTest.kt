package bose.ankush.home.domain.leaveby

import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LeaveByFakeDoorEligibilityTest {
    @Test
    fun eligible_whenTwoPlacesOneMetroMonsoonAndLeaveWindow() {
        val places =
            listOf(
                LeaveByPlace(lat = 12.97, lon = 77.59),
                LeaveByPlace(lat = 0.0, lon = 0.0),
            )

        assertTrue(LeaveByFakeDoorEligibility.isEligible(places, monsoonAt(hour = 16, minute = 0)))
    }

    @Test
    fun rejects_fewerThanTwoPlaces() {
        val places = listOf(LeaveByPlace(lat = 19.05, lon = 72.87))

        assertFalse(LeaveByFakeDoorEligibility.isEligible(places, monsoonAt(hour = 18, minute = 0)))
    }

    @Test
    fun rejects_twoPlacesOutsideEveryBox() {
        val places =
            listOf(
                LeaveByPlace(lat = 18.52, lon = 73.85),
                LeaveByPlace(lat = 13.08, lon = 80.27),
            )

        assertFalse(LeaveByFakeDoorEligibility.isEligible(places, monsoonAt(hour = 17, minute = 30)))
    }

    @Test
    fun timeWindow_includes1600AndExcludes2000() {
        val places = twoPlacesWithDelhi()

        assertFalse(LeaveByFakeDoorEligibility.isLeaveWindow(monsoonAt(hour = 15, minute = 59, second = 59)))
        assertTrue(LeaveByFakeDoorEligibility.isLeaveWindow(monsoonAt(hour = 16, minute = 0)))
        assertTrue(LeaveByFakeDoorEligibility.isLeaveWindow(monsoonAt(hour = 19, minute = 59, second = 59)))
        assertFalse(LeaveByFakeDoorEligibility.isLeaveWindow(monsoonAt(hour = 20, minute = 0)))

        assertFalse(LeaveByFakeDoorEligibility.isEligible(places, monsoonAt(hour = 15, minute = 59)))
        assertTrue(LeaveByFakeDoorEligibility.isEligible(places, monsoonAt(hour = 16, minute = 0)))
        assertTrue(LeaveByFakeDoorEligibility.isEligible(places, monsoonAt(hour = 19, minute = 59, second = 59)))
        assertFalse(LeaveByFakeDoorEligibility.isEligible(places, monsoonAt(hour = 20, minute = 0)))
    }

    @Test
    fun monsoon_isJuneThroughSeptemberOnly() {
        val places = twoPlacesWithHyderabad()

        assertFalse(LeaveByFakeDoorEligibility.isEligible(places, at(month = 5, hour = 17)))
        assertTrue(LeaveByFakeDoorEligibility.isEligible(places, at(month = 6, hour = 17)))
        assertTrue(LeaveByFakeDoorEligibility.isEligible(places, at(month = 9, hour = 17)))
        assertFalse(LeaveByFakeDoorEligibility.isEligible(places, at(month = 10, hour = 17)))
    }

    @Test
    fun boxes_matchCoordinatesNotNames_andIncludeEdges() {
        val midpointLon =
            (LeaveByFakeDoorEligibility.BENGALURU.minLon + LeaveByFakeDoorEligibility.BENGALURU.maxLon) / 2
        val onEdge =
            LeaveByPlace(
                lat = LeaveByFakeDoorEligibility.BENGALURU.minLat,
                lon = midpointLon,
            )
        val justSouth =
            LeaveByPlace(
                lat = LeaveByFakeDoorEligibility.BENGALURU.minLat - 0.001,
                lon = midpointLon,
            )

        assertTrue(
            LeaveByFakeDoorEligibility.isEligible(
                listOf(onEdge, LeaveByPlace(lat = 1.0, lon = 1.0)),
                monsoonAt(hour = 18, minute = 0),
            ),
        )
        assertFalse(
            LeaveByFakeDoorEligibility.isEligible(
                listOf(justSouth, LeaveByPlace(lat = 1.0, lon = 1.0)),
                monsoonAt(hour = 18, minute = 0),
            ),
        )
        assertTrue(LeaveByFakeDoorEligibility.MUMBAI.contains(19.05, 72.87))
        assertTrue(LeaveByFakeDoorEligibility.DELHI.contains(28.61, 77.21))
        assertTrue(LeaveByFakeDoorEligibility.HYDERABAD.contains(17.40, 78.48))
        assertFalse(LeaveByFakeDoorEligibility.MUMBAI.contains(19.05, 73.20))
    }

    private fun twoPlacesWithDelhi(): List<LeaveByPlace> =
        listOf(
            LeaveByPlace(lat = 28.61, lon = 77.21),
            LeaveByPlace(lat = 0.0, lon = 0.0),
        )

    private fun twoPlacesWithHyderabad(): List<LeaveByPlace> =
        listOf(
            LeaveByPlace(lat = 17.40, lon = 78.48),
            LeaveByPlace(lat = 0.0, lon = 0.0),
        )

    private fun monsoonAt(
        hour: Int,
        minute: Int,
        second: Int = 0,
    ): LocalDateTime = at(month = 7, hour = hour, minute = minute, second = second)

    private fun at(
        month: Int,
        hour: Int,
        minute: Int = 0,
        second: Int = 0,
    ): LocalDateTime =
        LocalDateTime(year = 2026, monthNumber = month, dayOfMonth = 15, hour = hour, minute = minute, second = second)
}
