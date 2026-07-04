package bose.ankush.finder.data.usecase

import bose.ankush.finder.domain.model.LocationSuggestion
import bose.ankush.finder.domain.repository.FinderRepository
import bose.ankush.finder.domain.usecase.SearchPlacesUseCase

internal class SearchPlacesUseCaseImpl(
    private val repository: FinderRepository,
) : SearchPlacesUseCase {
    override suspend fun invoke(query: String): Result<List<LocationSuggestion>> = repository.searchPlaces(query)
}
