package bose.ankush.network.domain

import bose.ankush.network.model.PlaceSuggestion
import bose.ankush.network.repository.LocationRepository

class SearchPlacesUseCase(
    private val repository: LocationRepository,
) {
    suspend operator fun invoke(query: String): Result<List<PlaceSuggestion>> =
        repository.searchPlaces(query)
}
