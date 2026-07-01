package bose.ankush.weatherify.domain.use_case

import bose.ankush.network.model.PlaceSuggestion
import bose.ankush.network.repository.LocationRepository
import javax.inject.Inject

class SearchPlacesUseCase
    @Inject
    constructor(
        private val repository: LocationRepository,
    ) {
        suspend operator fun invoke(query: String): Result<List<PlaceSuggestion>> = repository.searchPlaces(query)
    }
