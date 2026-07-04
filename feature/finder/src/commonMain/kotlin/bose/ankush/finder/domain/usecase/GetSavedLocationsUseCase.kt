package bose.ankush.finder.domain.usecase

import bose.ankush.finder.domain.model.Location

interface GetSavedLocationsUseCase {
    suspend operator fun invoke(): Result<List<Location>>
}
