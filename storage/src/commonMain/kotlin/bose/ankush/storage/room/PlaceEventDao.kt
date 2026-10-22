package bose.ankush.storage.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import bose.ankush.storage.common.PLACE_EVENTS_TABLE
import bose.ankush.storage.common.SYNC_CHECKPOINTS_TABLE
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceEventDao {
    @Query(
        "SELECT * FROM $PLACE_EVENTS_TABLE " +
            "WHERE deletedAt IS NULL AND syncState != 'PENDING_DELETE' ORDER BY startsAt",
    )
    fun observeVisible(): Flow<List<PlaceEventEntity>>

    @Query("SELECT * FROM $PLACE_EVENTS_TABLE WHERE id = :id")
    suspend fun getById(id: String): PlaceEventEntity?

    @Query("SELECT * FROM $PLACE_EVENTS_TABLE WHERE syncState != 'SYNCED' ORDER BY updatedAt")
    suspend fun getPending(): List<PlaceEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: PlaceEventEntity)

    @Query("DELETE FROM $PLACE_EVENTS_TABLE WHERE id = :id")
    suspend fun deleteById(id: String)

    @Transaction
    suspend fun rekey(
        oldId: String,
        event: PlaceEventEntity,
    ) {
        deleteById(oldId)
        upsert(event)
    }

    @Query("DELETE FROM $PLACE_EVENTS_TABLE")
    suspend fun deleteAllEvents()

    @Query("DELETE FROM $SYNC_CHECKPOINTS_TABLE")
    suspend fun deleteAllCheckpoints()

    /** Events and checkpoints go together, so a cleared store never resumes from an old cursor. */
    @Transaction
    suspend fun clearAll() {
        deleteAllEvents()
        deleteAllCheckpoints()
    }
}

@Dao
interface SyncCheckpointDao {
    @Query("SELECT checkpoint FROM $SYNC_CHECKPOINTS_TABLE WHERE stream = :stream")
    suspend fun get(stream: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(checkpoint: SyncCheckpointEntity)
}
