package bose.ankush.home.domain.usecase

import bose.ankush.home.domain.location.ActiveCoordinates
import bose.ankush.home.domain.location.CoordinateResult
import bose.ankush.home.domain.location.Coordinates
import bose.ankush.home.domain.location.isServedBy
import bose.ankush.home.domain.location.point
import bose.ankush.home.domain.model.ForecastPlace
import bose.ankush.home.domain.model.HomeWeather
import bose.ankush.home.domain.model.RefreshOutcome
import bose.ankush.home.domain.model.WeatherForecast
import bose.ankush.home.domain.model.WeatherSnapshot
import bose.ankush.home.domain.nearby.GeoPoint
import bose.ankush.home.domain.repository.WeatherRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveHomeWeatherTest {
    @Test
    fun placeIsTheSavedRowsOwn() =
        runTest {
            val repository = FakeRepository(row(at = GPS_POINT, name = "Kolkata"))
            val latest = collect(ObserveHomeWeather(repository, FixedCoordinates(GPS)))
            advanceUntilIdle()

            assertEquals(
                ForecastPlace(name = "Kolkata", isPinned = false, point = GPS_POINT),
                latest().place
            )
            assertEquals(HomeWeather.Status.Ready, latest().status)
            latest.stop()
        }

    @Test
    fun rowAtThePinnedSpotIsPinned() =
        runTest {
            val pinned = GPS.copy(isOverridden = true, overrideName = "Home")
            val repository = FakeRepository(row(at = GPS_POINT, name = "Home"))
            val latest = collect(ObserveHomeWeather(repository, FixedCoordinates(pinned)))
            advanceUntilIdle()

            assertEquals(
                ForecastPlace(name = "Home", isPinned = true, point = GPS_POINT),
                latest().place
            )
            latest.stop()
        }

    @Test
    fun rowFromBeforeSchema6TakesTheSavedSpot() =
        runTest {
            val pinned = GPS.copy(isOverridden = true, overrideName = "Home")
            val repository = FakeRepository(row(at = null, name = null))
            repository.onRefresh = { awaitCancellation() }
            val latest = collect(ObserveHomeWeather(repository, FixedCoordinates(pinned)))
            advanceUntilIdle()

            assertEquals(
                ForecastPlace(name = "Home", isPinned = true, point = GPS_POINT),
                latest().place
            )
            // Its place is assumed, not known, so the attempt waits for the refresh.
            assertEquals(HomeWeather.Status.Pending, latest().status)
            latest.stop()
        }

    /** The row is shown under its own place until the refresh writes the target's. */
    @Test
    fun rowForAnotherPlaceKeepsItsLabelUntilTheRefreshLands() =
        runTest {
            val release = CompletableDeferred<Unit>()
            val repository = FakeRepository(row(at = PARIS_POINT, name = "Paris"))
            repository.onRefresh = { release.await() }
            val latest = collect(ObserveHomeWeather(repository, FixedCoordinates(GPS)))
            advanceUntilIdle()

            assertEquals("Paris", latest().place?.name)
            assertEquals(HomeWeather.Status.Pending, latest().status)

            release.complete(Unit)
            advanceUntilIdle()
            assertEquals(GPS_POINT, latest().place?.point)
            assertEquals(HomeWeather.Status.Ready, latest().status)
            latest.stop()
        }

    @Test
    fun aNewAttemptCancelsTheRefreshInFlight() =
        runTest {
            val repository = FakeRepository()
            repository.onRefresh = { awaitCancellation() }
            val refreshes = MutableSharedFlow<Boolean>()
            val latest = collect(ObserveHomeWeather(repository, FixedCoordinates(GPS)), refreshes)
            advanceUntilIdle()
            val first = repository.inFlight.single()
            assertTrue(latest().isFetching)

            repository.onRefresh = {}
            refreshes.emit(true)
            advanceUntilIdle()

            assertTrue(first.isCancelled)
            assertEquals(2, repository.inFlight.size)
            assertFalse(latest().isFetching)
            latest.stop()
        }

    /**
     * Collects until [Collected.stop]. In the foreground on purpose: advanceUntilIdle does not
     * drive backgroundScope work.
     */
    private fun TestScope.collect(
        useCase: ObserveHomeWeather,
        refreshes: Flow<Boolean> = emptyFlow(),
    ): Collected {
        val collected = Collected()
        collected.job = launch { useCase(refreshes).collect { collected.latest = it } }
        return collected
    }

    private class Collected {
        var latest = HomeWeather()
        lateinit var job: Job

        operator fun invoke() = latest

        fun stop() = job.cancel()
    }

    private class FixedCoordinates(
        private val spot: ActiveCoordinates,
    ) : GetActiveCoordinates {
        override suspend fun current() = CoordinateResult(spot)

        override suspend fun lastKnown() = spot

        override fun changes(): Flow<Unit> = emptyFlow()

        override suspend fun reset() = Unit
    }

    /** Room's single row. A refresh that runs writes one for the place it was asked about. */
    private class FakeRepository(
        initial: WeatherSnapshot? = row(at = GPS_POINT, name = null),
    ) : WeatherRepository {
        override val snapshot = MutableStateFlow(initial)
        var onRefresh: suspend () -> Unit = {}
        val inFlight = mutableListOf<CompletableDeferred<Unit>>()

        override suspend fun refresh(
            place: ActiveCoordinates,
            forceRefresh: Boolean,
        ): Result<RefreshOutcome> {
            val call = CompletableDeferred<Unit>()
            inFlight += call
            try {
                onRefresh()
            } catch (e: Throwable) {
                call.cancel()
                throw e
            }
            call.complete(Unit)
            // Like the real repository: a fresh row for this place is kept as it is.
            if (!forceRefresh && place.isServedBy(snapshot.value?.point)) return Result.success(
                RefreshOutcome.UpToDate
            )
            snapshot.value = row(at = place.point, name = place.overrideName)
            return Result.success(RefreshOutcome.Updated)
        }

        override suspend fun clearAllData() = Unit
    }

    private companion object {
        val GPS_POINT = GeoPoint(22.57, 88.36)
        val PARIS_POINT = GeoPoint(48.8566, 2.3522)
        val GPS = ActiveCoordinates(Coordinates(GPS_POINT.lat, GPS_POINT.lon))

        fun row(
            at: GeoPoint?,
            name: String?,
        ) = WeatherSnapshot(
            WeatherForecast(id = 1L),
            airQuality = null,
            point = at,
            placeName = name
        )
    }
}
