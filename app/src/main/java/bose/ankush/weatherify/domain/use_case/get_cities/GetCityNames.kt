package bose.ankush.weatherify.domain.use_case.get_cities

import bose.ankush.weatherify.data.remote.dto.toCityName
import bose.ankush.weatherify.domain.model.CityName
import bose.ankush.weatherify.domain.repository.CityRepository
import javax.inject.Inject

class GetCityNames @Inject constructor(
    private val repository: CityRepository
) {
    operator fun invoke(): List<CityName> = try {
        val cityNames = repository.getCityNames().map { it.toCityName() }
        cityNames.ifEmpty { emptyList() }
    } catch (e: Exception) {
        emptyList()
    }
}