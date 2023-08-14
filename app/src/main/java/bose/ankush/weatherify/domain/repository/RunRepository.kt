package bose.ankush.weatherify.domain.repository

import bose.ankush.weatherify.data.room.run.RunEntity
import kotlinx.coroutines.flow.Flow

interface RunRepository {

    fun insertRun(runEntity: RunEntity)

    fun getAllRunsSortedByDate(): Flow<List<RunEntity>>

    fun getAllRunsSortedByTimeInMillis(): Flow<List<RunEntity>>

    fun getAllRunsSortedByCaloriesBurnt(): Flow<List<RunEntity>>

    fun getAllRunsSortedByAvgSpeed(): Flow<List<RunEntity>>

    fun getAllRunsSortedByDistance(): Flow<List<RunEntity>>

    fun getTotalRunTimeInMillis(): Flow<Long>

    fun getTotalCaloriesBurnt(): Flow<Int>

    fun getTotalAvgSpeed(): Flow<Float>

    fun getTotalDistance(): Flow<Int>
}