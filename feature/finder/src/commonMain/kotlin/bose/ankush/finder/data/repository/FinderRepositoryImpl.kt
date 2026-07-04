package bose.ankush.finder.data.repository

import bose.ankush.finder.data.mapper.toDomain
import bose.ankush.finder.domain.model.Location
import bose.ankush.finder.domain.model.LocationSuggestion
import bose.ankush.finder.domain.repository.FinderRepository
import bose.ankush.network.repository.LocationRepository

internal class FinderRepositoryImpl(
    private val networkRepository: LocationRepository,
) : FinderRepository {
    override suspend fun saveLocation(
        name: String,
        lat: Double,
        lon: Double,
    ): Result<Unit> = networkRepository.saveLocation(name, lat, lon)

    override suspend fun getSavedLocations(): Result<List<Location>> =
        networkRepository.getSavedLocations().map { dtoList ->
            dtoList.map { it.toDomain() }
        }

    override suspend fun deleteLocation(id: String): Result<Unit> = networkRepository.deleteLocation(id)

    override suspend fun searchPlaces(query: String): Result<List<LocationSuggestion>> =
        networkRepository.searchPlaces(query).map { dtoList ->
            dtoList.map { it.toDomain() }
        }
}
