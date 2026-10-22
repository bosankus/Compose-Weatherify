package bose.ankush.home.data.events

import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Time and ids for local writes; replaced in tests. */
internal interface PlaceEventClock {
    fun now(): Instant

    /** A new lowercase client UUID, the id POST /place-events expects. */
    fun newId(): String

    companion object System : PlaceEventClock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())

        @OptIn(ExperimentalUuidApi::class)
        override fun newId(): String = Uuid.random().toString().lowercase()
    }
}
