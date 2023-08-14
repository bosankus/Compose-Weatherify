package bose.ankush.weatherify.data.room.run

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Created by Androidplay
 * Author: Ankush
 * On: 8/14/2023, 01:23 AM
 */

@Database(entities = [RunEntity::class], version = 1, exportSchema = false)
@TypeConverters(RunDataModelConverters::class)
abstract class RunDatabase : RoomDatabase() {

    abstract fun runDao(): RunDao
}