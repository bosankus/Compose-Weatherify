package bose.ankush.home.domain.leaveby

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LeaveByFakeDoorEligibilityTest {
    @Test
    fun eligible_whenAtLeastTwoPlaces_regardlessOfCoordinates() {
        val places =
            listOf(
                LeaveByPlace(lat = 22.57, lon = 88.36),
                LeaveByPlace(lat = 0.0, lon = 0.0),
            )

        assertTrue(LeaveByFakeDoorEligibility.isEligible(places))
    }

    @Test
    fun rejects_fewerThanTwoPlaces() {
        assertFalse(LeaveByFakeDoorEligibility.isEligible(emptyList()))
        assertFalse(
            LeaveByFakeDoorEligibility.isEligible(listOf(LeaveByPlace(lat = 12.97, lon = 77.59))),
        )
    }

    @Test
    fun eligible_exactlyTwoPlaces() {
        val places =
            listOf(
                LeaveByPlace(lat = 1.0, lon = 1.0),
                LeaveByPlace(lat = 2.0, lon = 2.0),
            )

        assertTrue(LeaveByFakeDoorEligibility.isEligible(places))
        assertEquals(2, LeaveByFakeDoorEligibility.MIN_SAVED_PLACES)
    }
}
