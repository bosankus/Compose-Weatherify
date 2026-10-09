package bose.ankush.home.domain.usecase

import bose.ankush.home.domain.nearby.GeoPoint
import bose.ankush.home.domain.nearby.NearbyEvent
import bose.ankush.network.model.PlaceEvent
import bose.ankush.network.repository.PlaceEventRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * GET /place-events around [GeoPoint]. The server matches within about 1 km, so an empty list
 * means nothing is nearby. Events whose start time does not parse are dropped, never dated.
 *
 * Runs on [ioDispatcher]. `withContext` also gives prompt cancellation: a caller that has
 * moved on to another location never receives this result.
 */
internal class GetNearbyEvents(
    private val repository: PlaceEventRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val timeZone: () -> TimeZone = TimeZone::currentSystemDefault,
) {
    suspend operator fun invoke(point: GeoPoint): Result<List<NearbyEvent>> =
        withContext(ioDispatcher) {
            val zone = timeZone()
            repository
                .getPlaceEvents(point.lat, point.lon)
                .map { events -> events.mapNotNull { it.toNearbyEvent(zone) } }
        }
}

private fun PlaceEvent.toNearbyEvent(zone: TimeZone): NearbyEvent? {
    val instant = runCatching { Instant.parse(startsAt) }.getOrNull() ?: return null
    return NearbyEvent(title = title.trim(), date = instant.toLocalDateTime(zone).date)
}
