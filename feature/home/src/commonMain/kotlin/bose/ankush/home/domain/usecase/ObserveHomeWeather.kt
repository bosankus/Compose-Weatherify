package bose.ankush.home.domain.usecase

import bose.ankush.home.domain.location.ActiveCoordinates
import bose.ankush.home.domain.location.isServedBy
import bose.ankush.home.domain.location.point
import bose.ankush.home.domain.model.ForecastPlace
import bose.ankush.home.domain.model.HomeWeather
import bose.ankush.home.domain.model.HomeWeather.Status
import bose.ankush.home.domain.model.WeatherSnapshot
import bose.ankush.home.domain.repository.WeatherRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Home's weather as one stream: which place, which forecast, and how far the latest attempt
 * got. Coordinates are resolved here and never taken as input.
 *
 * One attempt runs on collection. Each value of `refreshes` starts another (`true` is pull to
 * refresh, which also skips the staleness check), and so does a change of pinned place. A new
 * attempt cancels the one in flight, including its network refresh.
 */
internal class ObserveHomeWeather(
    private val repository: WeatherRepository,
    private val coordinates: GetActiveCoordinates,
) {
    operator fun invoke(refreshes: Flow<Boolean>): Flow<HomeWeather> =
        channelFlow {
            val session = Session(channel)
            session.publish { it }
            merge(refreshes, coordinates.changes().map { true })
                .onStart { emit(false) }
                .collectLatest { userInitiated -> session.attempt(userInitiated) }
        }.buffer(Channel.UNLIMITED)
            .distinctUntilChanged()

    /** One collection's state. [publish] never suspends, so `finally` blocks can report too. */
    private inner class Session(
        private val out: SendChannel<HomeWeather>,
    ) {
        private val state = MutableStateFlow(HomeWeather())

        /** The active place as last resolved: pinned, GPS, or the saved spot. Decides `isPinned`. */
        private var spot: ActiveCoordinates? = null

        fun publish(change: (HomeWeather) -> HomeWeather) {
            state.update(change)
            out.trySend(state.value)
        }

        suspend fun attempt(userInitiated: Boolean) {
            publish { it.copy(status = Status.Pending) }
            try {
                paintSaved()
                val result = coordinates.current()
                val target = result.coordinates
                if (target != null) spot = target
                publish {
                    it.copy(
                        place = it.snapshot?.placeFor(spot),
                        locationProblem = result.problem,
                        status = if (target == null) Status.NoLocation else it.status,
                    )
                }
                if (target != null) fetch(target, userInitiated)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                publish { it.copy(status = Status.Failed(e)) }
            }
        }

        /**
         * Shows the Room forecast before saved coordinates, GPS, or the network. The row names
         * its own place; the saved spot then only says whether that place is the pinned one.
         */
        private suspend fun paintSaved() {
            if (state.value.snapshot != null) return
            try {
                val saved = repository.snapshot.first() ?: return
                publish {
                    it.copy(
                        snapshot = saved,
                        place = saved.placeFor(spot),
                        cacheChecked = true
                    )
                }
                spot = spot ?: coordinates.lastKnown() ?: return
                publish { it.copy(place = saved.placeFor(spot)) }
            } finally {
                if (!state.value.cacheChecked) publish { it.copy(cacheChecked = true) }
            }
        }

        /**
         * Refreshes for [target] while showing whatever Room holds. The row labels itself, so it
         * is never shown under the wrong place; the attempt settles once the row is [target]'s,
         * or once the refresh is over either way.
         */
        private suspend fun fetch(
            target: ActiveCoordinates,
            userInitiated: Boolean,
        ) = coroutineScope {
            val settled = MutableStateFlow(false)
            var failure: Throwable? = null
            launch {
                publish { it.copy(isFetching = true) }
                try {
                    failure = repository.refresh(target, userInitiated).exceptionOrNull()
                } finally {
                    publish { it.copy(isFetching = false) }
                    settled.value = true
                }
            }
            combine(repository.snapshot, settled, ::Pair).collect { (snapshot, done) ->
                val failed = failure.takeIf { done }
                val isTargets = snapshot != null && target.isServedBy(snapshot.point)
                publish {
                    it.copy(
                        snapshot = snapshot,
                        place = snapshot?.placeFor(spot),
                        status =
                            when {
                                failed != null -> Status.Failed(failed)
                                isTargets || done -> Status.Ready
                                else -> it.status
                            },
                    )
                }
            }
        }
    }
}

/**
 * The place this row is for. Rows from before schema 6 have no point, so they take the saved
 * spot, as home always assumed. Pinned only when the row is exactly the pinned spot.
 */
private fun WeatherSnapshot.placeFor(spot: ActiveCoordinates?): ForecastPlace? {
    val at = point ?: spot?.point ?: return null
    val pinned = spot != null && spot.isOverridden && spot.point == at
    return ForecastPlace(
        name = placeName ?: spot?.overrideName?.takeIf { pinned && it.isNotBlank() },
        isPinned = pinned,
        point = at,
    )
}
