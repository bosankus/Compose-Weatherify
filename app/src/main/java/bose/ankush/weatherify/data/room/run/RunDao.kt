package bose.ankush.weatherify.data.room.run

import androidx.room.*
import bose.ankush.weatherify.base.common.RUN_DATABASE_NAME
import kotlinx.coroutines.flow.Flow

/**
 * Created by Androidplay
 * Author: Ankush
 * On: 8/14/2023, 01:23 AM
 */

@Dao
interface RunDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRun(runEntity: RunEntity)

    @Query("SELECT * FROM $RUN_DATABASE_NAME ORDER BY timestamp DESC")
    fun getAllRunsSortedByDate(): Flow<List<RunEntity>>

    @Query("SELECT * FROM $RUN_DATABASE_NAME ORDER BY timeInMillis DESC")
    fun getAllRunsSortedByTimeInMillis(): Flow<List<RunEntity>>

    @Query("SELECT * FROM $RUN_DATABASE_NAME ORDER BY caloriesBurnt DESC")
    fun getAllRunsSortedByCaloriesBurnt(): Flow<List<RunEntity>>

    @Query("SELECT * FROM $RUN_DATABASE_NAME ORDER BY avgSpeedInKMH DESC")
    fun getAllRunsSortedByAvgSpeed(): Flow<List<RunEntity>>

    @Query("SELECT * FROM $RUN_DATABASE_NAME ORDER BY distanceInMeters DESC")
    fun getAllRunsSortedByDistance(): Flow<List<RunEntity>>

    @Query("SELECT SUM(timeInMillis) FROM $RUN_DATABASE_NAME")
    fun getTotalRunTimeInMillis(): Flow<Long>

    @Query("SELECT SUM(caloriesBurnt) FROM $RUN_DATABASE_NAME")
    fun getTotalCaloriesBurnt(): Flow<Int>

    @Query("SELECT AVG(avgSpeedInKMH) FROM $RUN_DATABASE_NAME")
    fun getTotalAvgSpeed(): Flow<Float>

    @Query("SELECT SUM(distanceInMeters) FROM $RUN_DATABASE_NAME")
    fun getTotalDistance(): Flow<Int>

    // Not required at this moment
    /*@Delete
    suspend fun deleteRun(runEntity: RunEntity)*/
}