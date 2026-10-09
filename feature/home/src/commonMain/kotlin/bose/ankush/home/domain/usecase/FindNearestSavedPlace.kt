package bose.ankush.home.domain.usecase

import bose.ankush.home.domain.nearby.GeoPoint
import bose.ankush.home.domain.nearby.distanceMetersTo
import bose.ankush.network.model.SavedLocation
import bose.ankush.network.repository.LocationRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * The user's saved place closest to [GeoPoint], within [SAVED_PLACE_MATCH_METERS], or null
 * when none qualifies. Runs on [ioDispatcher].
 */
internal class FindNearestSavedPlace(
    private val repository: LocationRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend operator fun invoke(point: GeoPoint): Result<SavedLocation?> =
        withContext(ioDispatcher) {
            repository.getSavedLocations().map { places -> nearestSavedPlace(places, point) }
        }
}

internal fun nearestSavedPlace(
    places: List<SavedLocation>,
    point: GeoPoint,
    withinMeters: Double = SAVED_PLACE_MATCH_METERS,
): SavedLocation? =
    places
        .map { place -> place to point.distanceMetersTo(GeoPoint(place.lat, place.lon)) }
        .filter { (_, meters) -> meters <= withinMeters }
        .minByOrNull { (_, meters) -> meters }
        ?.first

/**
 * "Closely" was not given a radius. One kilometre is an assumption, the same order of
 * magnitude as the place-events match, not a value from the contract.
 */
internal const val SAVED_PLACE_MATCH_METERS = 1_000.0
