package bose.ankush.home.data.events

import bose.ankush.home.domain.events.PlaceEventInput
import bose.ankush.home.domain.events.PlaceEventItem
import bose.ankush.home.domain.events.PlaceEventSyncStatus
import bose.ankush.home.domain.nearby.GeoPoint
import bose.ankush.network.model.CreatePlaceEventBody
import bose.ankush.network.model.PlaceEventDto
import bose.ankush.network.model.UpdatePlaceEventBody
import bose.ankush.storage.model.PlaceEventRecord
import bose.ankush.storage.model.PlaceEventSyncState
import kotlin.time.Instant

/** The stored row as the domain sees it; null when its start time does not parse. */
internal fun PlaceEventRecord.toItem(): PlaceEventItem? =
    parseInstant(startsAt)?.let { start ->
        PlaceEventItem(
            id = id,
            title = title,
            startsAt = start,
            note = note,
            placeName = placeName,
            point = GeoPoint(lat, lon),
            syncStatus = syncState.toStatus(),
            syncError = syncError.takeIf { syncState == PlaceEventSyncState.FAILED },
        )
    }

private fun PlaceEventSyncState.toStatus(): PlaceEventSyncStatus =
    when (this) {
        PlaceEventSyncState.SYNCED -> PlaceEventSyncStatus.SYNCED
        PlaceEventSyncState.PENDING_UPSERT, PlaceEventSyncState.PENDING_DELETE -> PlaceEventSyncStatus.PENDING
        PlaceEventSyncState.FAILED -> PlaceEventSyncStatus.FAILED
    }

/** [input] written over this row's editable fields. */
internal fun PlaceEventRecord.withInput(input: PlaceEventInput): PlaceEventRecord =
    copy(
        title = input.title.trim(),
        startsAt = input.startsAt.toString(),
        note = input.note?.trim()?.takeIf { it.isNotEmpty() },
        placeName = input.placeName,
        lat = input.point.lat,
        lon = input.point.lon,
    )

/** A server row, reconciled: [PlaceEventSyncState.SYNCED] with its `updatedAt` as the PUT base. */
internal fun PlaceEventDto.toSyncedRecord(): PlaceEventRecord =
    PlaceEventRecord(
        id = id,
        title = title,
        startsAt = startsAt,
        note = note,
        placeName = placeName,
        lat = lat,
        lon = lon,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = null,
        syncState = PlaceEventSyncState.SYNCED,
        serverUpdatedAt = updatedAt,
    )

/**
 * A server copy whose local row is already gone (deleted before the create was acknowledged),
 * queued for DELETE and hidden like any pending delete.
 */
internal fun PlaceEventDto.toPendingDelete(): PlaceEventRecord =
    toSyncedRecord().copy(deletedAt = updatedAt, syncState = PlaceEventSyncState.PENDING_DELETE)

/** True when the server copy holds the same editable fields as this row. */
internal fun PlaceEventRecord.hasSameContentAs(dto: PlaceEventDto): Boolean =
    title == dto.title &&
        startsAt == dto.startsAt &&
        note == dto.note &&
        placeName == dto.placeName &&
        lat == dto.lat &&
        lon == dto.lon

internal fun PlaceEventRecord.toCreateBody(): CreatePlaceEventBody =
    CreatePlaceEventBody(
        id = id,
        placeName = placeName,
        lat = lat,
        lon = lon,
        title = title,
        startsAt = startsAt,
        note = note,
    )

internal fun PlaceEventRecord.toUpdateBody(): UpdatePlaceEventBody =
    UpdatePlaceEventBody(
        placeName = placeName,
        lat = lat,
        lon = lon,
        title = title,
        startsAt = startsAt,
        note = note,
        baseUpdatedAt = serverUpdatedAt,
    )

internal fun parseInstant(text: String?): Instant? = text?.let { runCatching { Instant.parse(it.trim()) }.getOrNull() }
