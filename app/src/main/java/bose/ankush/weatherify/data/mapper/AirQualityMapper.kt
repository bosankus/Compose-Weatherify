package bose.ankush.weatherify.data.mapper

import bose.ankush.weatherify.domain.model.AirQuality
import bose.ankush.storage.model.AirQualityData as StorageAirQualityData

object AirQualityMapper {
    fun mapToDomain(entity: StorageAirQualityData): AirQuality =
        AirQuality(
            id = entity.id,
            aqi = entity.aqi ?: 0,
            co = entity.co ?: 0.0,
            no2 = entity.no2 ?: 0.0,
            o3 = entity.o3 ?: 0.0,
            so2 = entity.so2 ?: 0.0,
            pm10 = entity.pm10 ?: 0.0,
            pm25 = entity.pm25 ?: 0.0,
        )
}
