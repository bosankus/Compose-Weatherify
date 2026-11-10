package bose.ankush.home.data.mapper

import bose.ankush.home.domain.model.AirQuality
import bose.ankush.storage.model.AirQualityData as StorageAirQualityData

internal object AirQualityMapper {
    /** Null when there is no index: the free tier saves an empty row, and AQI is 1 to 5. */
    fun mapToDomain(entity: StorageAirQualityData): AirQuality? {
        val aqi = entity.aqi?.takeIf { it > 0 } ?: return null
        return AirQuality(
            id = entity.id,
            aqi = aqi,
            co = entity.co ?: 0.0,
            no2 = entity.no2 ?: 0.0,
            o3 = entity.o3 ?: 0.0,
            so2 = entity.so2 ?: 0.0,
            pm10 = entity.pm10 ?: 0.0,
            pm25 = entity.pm25 ?: 0.0,
        )
    }
}
