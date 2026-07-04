package bose.ankush.finder.domain.usecase

interface SaveLocationUseCase {
    suspend operator fun invoke(params: SaveLocationParams): Result<Unit>
}

data class SaveLocationParams(
    val name: String,
    val lat: Double,
    val lon: Double,
)
