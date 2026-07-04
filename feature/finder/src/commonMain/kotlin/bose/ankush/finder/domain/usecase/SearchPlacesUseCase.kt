package bose.ankush.finder.domain.usecase

import bose.ankush.finder.domain.model.LocationSuggestion

interface SearchPlacesUseCase {
    suspend operator fun invoke(query: String): Result<List<LocationSuggestion>>
}
