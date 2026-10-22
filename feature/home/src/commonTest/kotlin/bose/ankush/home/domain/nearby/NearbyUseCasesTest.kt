package bose.ankush.home.domain.nearby

import bose.ankush.home.domain.usecase.CreateNearbyEvent
import bose.ankush.home.domain.usecase.GetNearbyEvents
import bose.ankush.home.domain.usecase.nearestSavedPlace
import bose.ankush.network.model.PlaceEvent
import bose.ankush.network.model.SavedLocation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NearbyUseCasesTest {
    @Test
    fun savedPlaceMatchesTheClosestWithinOneKilometreOnly() {
        val near = SavedLocation(id = "a", name = "Near", lat = 10.0, lon = 10.0)
        val far = SavedLocation(id = "b", name = "Far", lat = 10.02, lon = 10.0)

        assertEquals("Near", nearestSavedPlace(listOf(far, near), GeoPoint(10.001, 10.0))?.name)
        assertNull(nearestSavedPlace(listOf(far), GeoPoint(10.0, 10.0)))
    }

    @Test
    fun nearbyEventsKeepTheLocalDateAndBlankTitlesAndDropUnparseableStarts() =
        runTest {
            val repository =
                FakePlaceEventRepository(
                    events = { _, _ ->
                        Result.success(
                            listOf(
                                PlaceEvent(startsAt = "2026-10-04T18:30:00Z", title = " Dinner "),
                                PlaceEvent(startsAt = "", title = "Broken"),
                                PlaceEvent(startsAt = "2026-10-05T08:00:00Z", title = " "),
                            ),
                        )
                    },
                )
            val getNearbyEvents =
                GetNearbyEvents(
                    repository,
                    StandardTestDispatcher(testScheduler),
                    timeZone = { TimeZone.UTC },
                )

            val events = getNearbyEvents(GeoPoint(1.0, 2.0)).getOrThrow()

            assertEquals(
                listOf(
                    NearbyEvent(title = "Dinner", startsAt = LocalDateTime(2026, 10, 4, 18, 30)),
                    NearbyEvent(title = "", startsAt = LocalDateTime(2026, 10, 5, 8, 0)),
                ),
                events,
            )
            assertEquals(listOf(1.0 to 2.0), repository.requested)
        }

    @Test
    fun anIncompleteDraftIsNeverPosted() =
        runTest {
            val repository = FakePlaceEventRepository()
            val create =
                CreateNearbyEvent(
                    repository,
                    StandardTestDispatcher(testScheduler),
                    timeZone = { TimeZone.UTC },
                )

            val result =
                create(NearbyPlace("Harbor", GeoPoint(1.0, 2.0)), EventDraft(title = "Dinner"))

            assertTrue(result.isFailure)
            assertTrue(repository.created.isEmpty())
        }

    @Test
    fun aCompleteDraftPostsTheTrimmedTitleAndTheInstant() =
        runTest {
            val repository = FakePlaceEventRepository()
            val create =
                CreateNearbyEvent(
                    repository,
                    StandardTestDispatcher(testScheduler),
                    timeZone = { TimeZone.UTC },
                )

            create(
                NearbyPlace("Harbor", GeoPoint(1.0, 2.0)),
                EventDraft(title = " Dinner ", dateText = "2026-10-04", timeText = "09:30"),
            ).getOrThrow()

            val request = repository.created.single()
            assertEquals("Dinner", request.title)
            assertEquals("Harbor", request.placeName)
            assertEquals("2026-10-04T09:30:00Z", request.startsAt)
            assertNull(request.note)
        }

    @Test
    fun aNoteIsTrimmedAndPostedWithTheEvent() =
        runTest {
            val repository = FakePlaceEventRepository()
            val create =
                CreateNearbyEvent(
                    repository,
                    StandardTestDispatcher(testScheduler),
                    timeZone = { TimeZone.UTC },
                )

            create(
                NearbyPlace("Harbor", GeoPoint(1.0, 2.0)),
                EventDraft(title = "Dinner", dateText = "2026-10-04", timeText = "09:30", note = "  Bring a jacket "),
            ).getOrThrow()

            assertEquals("Bring a jacket", repository.created.single().note)
        }

    @Test
    fun nearbyEventsKeepTheLocalStartTimeAndTheNote() =
        runTest {
            val repository =
                FakePlaceEventRepository(
                    events = { _, _ ->
                        Result.success(
                            listOf(
                                PlaceEvent(startsAt = "2026-10-04T18:30:00Z", title = "Dinner", note = " Table 4 "),
                                PlaceEvent(startsAt = "2026-10-04T20:00:00Z", title = "Walk", note = "  "),
                            ),
                        )
                    },
                )
            val kolkata = TimeZone.of("Asia/Kolkata")
            val getNearbyEvents =
                GetNearbyEvents(repository, StandardTestDispatcher(testScheduler), timeZone = { kolkata })

            val events = getNearbyEvents(GeoPoint(1.0, 2.0)).getOrThrow()

            // 18:30Z is midnight in Kolkata, so the date moves to the next day with the time.
            assertEquals(LocalDateTime(2026, 10, 5, 0, 0), events[0].startsAt)
            assertEquals(LocalDate(2026, 10, 5), events[0].date)
            assertEquals("Table 4", events[0].note)
            assertEquals(LocalTime(1, 30), events[1].startsAt.time)
            assertNull(events[1].note)
        }
}
