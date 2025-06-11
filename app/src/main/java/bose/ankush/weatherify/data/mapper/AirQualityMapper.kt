package bose.ankush.weatherify.data.mapper

import bose.ankush.weatherify.data.room.weather.AirQualityEntity
import bose.ankush.weatherify.domain.model.AirQuality

/**
 * Mapper class to convert between AirQualityEntity (data layer) and AirQuality (domain layer)
 */
object AirQualityMapper {
    
    /**
     * Maps an AirQualityEntity to an AirQuality domain model
     */
    fun mapToDomain(entity: AirQualityEntity): AirQuality {
        return AirQuality(
            id = entity.id,
            aqi = entity.aqi,
            co = entity.co,
            no2 = entity.no2,
            o3 = entity.o3,
            so2 = entity.so2,
            pm10 = entity.pm10,
            pm25 = entity.pm25
        )
    }
    
    /**
     * Maps an AirQuality domain model to an AirQualityEntity
     */
    fun mapToEntity(domain: AirQuality): AirQualityEntity {
        return AirQualityEntity(
            id = domain.id,
            aqi = domain.aqi,
            co = domain.co,
            no2 = domain.no2,
            o3 = domain.o3,
            so2 = domain.so2,
            pm10 = domain.pm10,
            pm25 = domain.pm25
        )
    }
}