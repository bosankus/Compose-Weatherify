package bose.ankush.storage

import bose.ankush.storage.model.PlaceEventRecord
import bose.ankush.storage.model.PlaceEventSyncState
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaceEventRecordMapperTest {
    private val record =
        PlaceEventRecord(
            id = "0b5d8a8e-5a3c-4c4f-9a0e-2f6f7f1d2c3b",
            title = "Dinner",
            startsAt = "2026-10-24T14:00:00Z",
            note = null,
            placeName = "Harbor",
            lat = 12.97,
            lon = 77.59,
            createdAt = "2026-10-22T10:00:00.000Z",
            updatedAt = "2026-10-22T10:00:00.000Z",
            syncState = PlaceEventSyncState.PENDING_DELETE,
            serverUpdatedAt = "2026-10-21T10:00:00.000Z",
        )

    @Test
    fun recordSurvivesTheRoomRow() {
        assertEquals(record, record.toEntity().toRecord())
        assertEquals("PENDING_DELETE", record.toEntity().syncState)
    }

    @Test
    fun anUnknownSyncStateIsKeptAsPendingSoItIsPushedNotLost() {
        val row = record.toEntity().copy(syncState = "SOMETHING_NEW")

        assertEquals(PlaceEventSyncState.PENDING_UPSERT, row.toRecord().syncState)
    }
}
