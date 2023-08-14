package bose.ankush.weatherify.data.repository

import bose.ankush.weatherify.data.room.run.RunDatabase
import bose.ankush.weatherify.data.room.run.RunEntity
import bose.ankush.weatherify.domain.repository.RunRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RunRepositoryImpl @Inject constructor(private val runDatabase: RunDatabase) : RunRepository {

    override fun insertRun(runEntity: RunEntity) {
        runDatabase.runDao().insertRun(runEntity)
    }

    override fun getAllRunsSortedByDate(): Flow<List<RunEntity>> =
        runDatabase.runDao().getAllRunsSortedByDate()

    override fun getAllRunsSortedByTimeInMillis(): Flow<List<RunEntity>> =
        runDatabase.runDao().getAllRunsSortedByTimeInMillis()

    override fun getAllRunsSortedByCaloriesBurnt(): Flow<List<RunEntity>> =
        runDatabase.runDao().getAllRunsSortedByCaloriesBurnt()

    override fun getAllRunsSortedByAvgSpeed(): Flow<List<RunEntity>> =
        runDatabase.runDao().getAllRunsSortedByAvgSpeed()

    override fun getAllRunsSortedByDistance(): Flow<List<RunEntity>> =
        runDatabase.runDao().getAllRunsSortedByDistance()

    override fun getTotalRunTimeInMillis(): Flow<Long> =
        runDatabase.runDao().getTotalRunTimeInMillis()

    override fun getTotalCaloriesBurnt(): Flow<Int> =
        runDatabase.runDao().getTotalCaloriesBurnt()

    override fun getTotalAvgSpeed(): Flow<Float> =
        runDatabase.runDao().getTotalAvgSpeed()

    override fun getTotalDistance(): Flow<Int> =
        runDatabase.runDao().getTotalDistance()
}