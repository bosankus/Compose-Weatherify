package bose.ankush.storage.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import bose.ankush.storage.common.AQ_DATABASE_NAME

@Entity(tableName = AQ_DATABASE_NAME)
data class AirQualityEntity(
    @PrimaryKey(autoGenerate = true) var id: Long? = null,
    var aqi: Int? = 0,
    var co: Double? = 0.0,
    var no2: Double? = 0.0,
    var o3: Double? = 0.0,
    var so2: Double? = 0.0,
    var pm10: Double? = 0.0,
    var pm25: Double? = 0.0,
)