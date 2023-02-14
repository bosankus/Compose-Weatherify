package bose.ankush.weatherify.domain.repository

import bose.ankush.weatherify.data.remote.dto.CityDto

interface CityRepository {
    fun getCityNames(): List<CityDto>
}