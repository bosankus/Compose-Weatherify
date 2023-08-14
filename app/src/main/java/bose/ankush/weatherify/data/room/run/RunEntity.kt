package bose.ankush.weatherify.data.room.run

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey
import bose.ankush.weatherify.base.common.RUN_DATABASE_NAME

/**
 * Created by Androidplay
 * Author: Ankush
 * On: 8/14/2023, 01:23 AM
 */

@Entity(tableName = RUN_DATABASE_NAME)
data class RunEntity(
    var img: Bitmap? = null,
    var timestamp: Long = 0L,
    var avgSpeedInKMH: Float = 0f,
    var distanceInMeters: Int = 0,
    var timeInMillis: Long = 0L,
    var caloriesBurnt: Int = 0
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null
}