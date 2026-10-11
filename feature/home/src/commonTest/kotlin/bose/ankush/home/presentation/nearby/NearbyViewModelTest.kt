package bose.ankush.home.presentation.nearby

import bose.ankush.home.domain.nearby.FakeLocationRepository
import bose.ankush.home.domain.nearby.FakePlaceEventRepository
import bose.ankush.home.domain.nearby.GeoPoint
import bose.ankush.home.domain.nearby.NearbyEvent
import bose.ankush.home.domain.usecase.CreateNearbyEvent
import bose.ankush.home.domain.usecase.FindNearestSavedPlace
import bose.ankush.home.domain.usecase.GetNearbyEvents
import bose.ankush.home.presentation.util.SectionState
import bose.ankush.network.model.PlaceEvent
import bose.ankush.network.model.SavedLocation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NearbyViewModelTest {
    private val here = GeoPoint(1.0, 2.0)
    private val there = GeoPoint(3.0, 4.0)

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun nothingLoadsUntilThereIsALocation() =
        runTest {
            val events = FakePlaceEventRepository()
            val places = FakeLocationRepository()
            nearbyViewModel(events, places)
            advanceUntilIdle()

            assertTrue(events.requested.isEmpty())
            assertEquals(0, places.savedCalls)
        }

    @Test
    fun aLocationLoadsEventsAndTheFeaturedPlace() =
        runTest {
            val events =
                FakePlaceEventRepository(events = { _, _ -> Result.success(listOf(dinner())) })
            val places =
                FakeLocationRepository(
                    Result.success(
                        listOf(
                            SavedLocation(
                                name = "Home",
                                lat = 1.0,
                                lon = 2.0
                            )
                        )
                    )
                )
            val viewModel = nearbyViewModel(events, places)

            viewModel.onIntent(NearbyIntent.LocationChanged(here))
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(
                SectionState.Loaded(listOf(NearbyEvent("Dinner", LocalDate(2026, 10, 4)))),
                state.events
            )
            assertEquals("Home", (state.featuredPlace as SectionState.Loaded).value?.name)
        }

    @Test
    fun theSameLocationTwiceDoesNotRefetch() =
        runTest {
            val events = FakePlaceEventRepository()
            val places = FakeLocationRepository()
            val viewModel = nearbyViewModel(events, places)

            viewModel.onIntent(NearbyIntent.LocationChanged(here))
            advanceUntilIdle()
            viewModel.onIntent(NearbyIntent.LocationChanged(here))
            advanceUntilIdle()

            assertEquals(1, events.requested.size)
            assertEquals(1, places.savedCalls)
        }

    @Test
    fun aSlowResponseForAnOldLocationNeverOverwritesTheNewOne() =
        runTest {
            val events =
                FakePlaceEventRepository(
                    events = { lat, _ ->
                        val title = if (lat == here.lat) "Old" else "New"
                        Result.success(
                            listOf(
                                PlaceEvent(
                                    startsAt = "2026-10-04T18:30:00Z",
                                    title = title
                                )
                            )
                        )
                    },
                )
            val slowHere = CompletableDeferred<Unit>()
            events.gates[here.lat to here.lon] = slowHere
            val viewModel = nearbyViewModel(events, FakeLocationRepository())

            viewModel.onIntent(NearbyIntent.LocationChanged(here))
            advanceUntilIdle()
            viewModel.onIntent(NearbyIntent.LocationChanged(there))
            advanceUntilIdle()
            slowHere.complete(Unit)
            advanceUntilIdle()

            val loaded = viewModel.state.value.events as SectionState.Loaded
            assertEquals(listOf("New"), loaded.value.map { it.title })
        }

    @Test
    fun retryRefetchesEventsOnly() =
        runTest {
            var fail = true
            val events =
                FakePlaceEventRepository(
                    events = { _, _ ->
                        if (fail) Result.failure(IllegalStateException("offline")) else Result.success(
                            listOf(dinner())
                        )
                    },
                )
            val places = FakeLocationRepository()
            val viewModel = nearbyViewModel(events, places)
            viewModel.onIntent(NearbyIntent.LocationChanged(here))
            advanceUntilIdle()
            assertEquals(SectionState.Failed, viewModel.state.value.events)

            fail = false
            viewModel.onIntent(NearbyIntent.RetryEvents)
            advanceUntilIdle()

            assertEquals(1, viewModel.state.value.eventDates.size)
            assertEquals(1, places.savedCalls)
        }

    @Test
    fun aSavedEventClosesTheFormAndReloadsEvents() =
        runTest {
            val events = FakePlaceEventRepository()
            val viewModel = nearbyViewModel(events, FakeLocationRepository())
            viewModel.fillComposer()
            advanceUntilIdle()

            viewModel.onIntent(NearbyIntent.SubmitEvent)
            advanceUntilIdle()

            assertEquals(1, events.created.size)
            assertFalse(viewModel.state.value.composer.isVisible)
            assertEquals(2, events.requested.size)
        }

    @Test
    fun aFailedSaveKeepsTheFormOpenWithTheError() =
        runTest {
            val events =
                FakePlaceEventRepository(createResult = Result.failure(IllegalStateException("No network")))
            val viewModel = nearbyViewModel(events, FakeLocationRepository())
            viewModel.fillComposer()
            advanceUntilIdle()

            viewModel.onIntent(NearbyIntent.SubmitEvent)
            advanceUntilIdle()

            val composer = viewModel.state.value.composer
            assertTrue(composer.isVisible)
            assertFalse(composer.isSubmitting)
            assertEquals("No network", composer.error)
        }

    @Test
    fun anIncompleteFormIsNeverSubmitted() =
        runTest {
            val events = FakePlaceEventRepository()
            val viewModel = nearbyViewModel(events, FakeLocationRepository())
            viewModel.onIntent(NearbyIntent.LocationChanged(here))
            viewModel.onIntent(NearbyIntent.PlaceNameChanged("Harbor"))
            viewModel.onIntent(NearbyIntent.TitleChanged("Dinner"))

            viewModel.onIntent(NearbyIntent.SubmitEvent)
            advanceUntilIdle()

            assertTrue(events.created.isEmpty())
        }

    private fun NearbyViewModel.fillComposer() {
        onIntent(NearbyIntent.LocationChanged(here))
        onIntent(NearbyIntent.PlaceNameChanged("Harbor"))
        onIntent(NearbyIntent.OpenComposer)
        onIntent(NearbyIntent.TitleChanged("Dinner"))
        onIntent(NearbyIntent.DateChanged("2026-10-04"))
        onIntent(NearbyIntent.TimeChanged("09:30"))
    }

    private fun dinner() = PlaceEvent(startsAt = "2026-10-04T18:30:00Z", title = "Dinner")

    private fun TestScope.nearbyViewModel(
        events: FakePlaceEventRepository,
        places: FakeLocationRepository,
    ): NearbyViewModel {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val utc = { TimeZone.UTC }
        return NearbyViewModel(
            getNearbyEvents = GetNearbyEvents(events, dispatcher, utc),
            findNearestSavedPlace = FindNearestSavedPlace(places, dispatcher),
            createNearbyEvent = CreateNearbyEvent(events, dispatcher, utc),
        )
    }
}
