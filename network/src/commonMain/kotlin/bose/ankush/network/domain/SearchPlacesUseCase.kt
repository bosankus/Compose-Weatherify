package bose.ankush.network.domain

import bose.ankush.network.model.PlaceSuggestion
import bose.ankush.network.repository.LocationRepository

/**
 * Use case for searching places by query string.
 * Wraps LocationRepository.searchPlaces with KMP-compatible interface.
 */
class SearchPlacesUseCase(
    private val repository: LocationRepository,
) {
    suspend operator fun invoke(query: String): Result<List<PlaceSuggestion>> =
        repository.searchPlaces(query)
}
