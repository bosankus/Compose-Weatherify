package bose.ankush.storage.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import bose.ankush.storage.common.PLACE_EVENTS_TABLE
import bose.ankush.storage.common.SYNC_CHECKPOINTS_TABLE

/**
 * Room row for [bose.ankush.storage.model.PlaceEventRecord]. [syncState] holds the
 * [bose.ankush.storage.model.PlaceEventSyncState] name as TEXT, so no TypeConverter is needed.
 * Keep the columns in step with [MIGRATION_5_6].
 */
@Entity(tableName = PLACE_EVENTS_TABLE)
data class PlaceEventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val startsAt: String,
    val note: String?,
    val placeName: String,
    val lat: Double,
    val lon: Double,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String?,
    val syncState: String,
    val serverUpdatedAt: String?,
)

/** A sync cursor per change feed ([stream]), e.g. the place-events checkpoint. */
@Entity(tableName = SYNC_CHECKPOINTS_TABLE)
data class SyncCheckpointEntity(
    @PrimaryKey val stream: String,
    val checkpoint: String,
)
