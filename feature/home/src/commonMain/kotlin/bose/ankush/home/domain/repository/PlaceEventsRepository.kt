package bose.ankush.home.domain.repository

import bose.ankush.home.domain.events.EventArea
import bose.ankush.home.domain.events.PlaceEventInput
import bose.ankush.home.domain.events.PlaceEventItem
import kotlinx.coroutines.flow.Flow

/**
 * Offline-first home place events. Reads come from the local store; writes land locally first,
 * marked pending, and a background sync pushes them to the server. Every write succeeds offline.
 */
internal interface PlaceEventsRepository {
    /** Events in [area], updated whenever the local store changes (local writes and synced pulls). */
    fun observeEvents(area: EventArea): Flow<List<PlaceEventItem>>

    suspend fun createEvent(input: PlaceEventInput): Result<PlaceEventItem>

    /** Replaces the editable fields of [id]. Fails when [id] is unknown or already deleted. */
    suspend fun updateEvent(
        id: String,
        input: PlaceEventInput,
    ): Result<PlaceEventItem>

    /** Hides [id] at once and deletes it on the server at the next sync. Unknown ids succeed. */
    suspend fun deleteEvent(id: String): Result<Unit>

    /** True once a sync has completed at least one pull (a checkpoint is stored). */
    suspend fun hasSynced(): Boolean

    /** Drops every event and the sync checkpoint (sign-out). */
    suspend fun clearAll()
}
