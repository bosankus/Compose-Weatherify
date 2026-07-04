package bose.ankush.finder.domain.usecase

interface DeleteLocationUseCase {
    suspend operator fun invoke(id: String): Result<Unit>
}
