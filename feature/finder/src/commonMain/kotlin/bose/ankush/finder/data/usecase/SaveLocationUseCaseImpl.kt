package bose.ankush.finder.data.usecase

import bose.ankush.finder.domain.repository.FinderRepository
import bose.ankush.finder.domain.usecase.SaveLocationParams
import bose.ankush.finder.domain.usecase.SaveLocationUseCase

internal class SaveLocationUseCaseImpl(
    private val repository: FinderRepository,
) : SaveLocationUseCase {
    override suspend fun invoke(params: SaveLocationParams): Result<Unit> =
        repository.saveLocation(params.name, params.lat, params.lon)
}
