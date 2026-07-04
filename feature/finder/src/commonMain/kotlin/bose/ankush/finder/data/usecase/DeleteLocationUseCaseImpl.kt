package bose.ankush.finder.data.usecase

import bose.ankush.finder.domain.repository.FinderRepository
import bose.ankush.finder.domain.usecase.DeleteLocationUseCase

internal class DeleteLocationUseCaseImpl(
    private val repository: FinderRepository,
) : DeleteLocationUseCase {
    override suspend fun invoke(id: String): Result<Unit> = repository.deleteLocation(id)
}
