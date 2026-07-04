package bose.ankush.finder.data.usecase

import bose.ankush.finder.domain.model.Location
import bose.ankush.finder.domain.repository.FinderRepository
import bose.ankush.finder.domain.usecase.GetSavedLocationsUseCase

internal class GetSavedLocationsUseCaseImpl(
    private val repository: FinderRepository,
) : GetSavedLocationsUseCase {
    override suspend fun invoke(): Result<List<Location>> = repository.getSavedLocations()
}
