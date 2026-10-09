package bose.ankush.home.domain.usecase

import bose.ankush.home.domain.nearby.EventDraft
import bose.ankush.home.domain.nearby.NearbyPlace
import bose.ankush.network.model.CreatePlaceEventRequest
import bose.ankush.network.repository.PlaceEventRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone

/** POST /place-events for [EventDraft] at [NearbyPlace]. An incomplete draft is never sent. */
internal class CreateNearbyEvent(
    private val repository: PlaceEventRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val timeZone: () -> TimeZone = TimeZone::currentSystemDefault,
) {
    suspend operator fun invoke(
        place: NearbyPlace,
        draft: EventDraft,
    ): Result<Unit> {
        val startsAt = draft.startsAt(timeZone())
        if (draft.title.isBlank() || startsAt == null) {
            return Result.failure(IllegalArgumentException("An event needs a title, a date, and a time."))
        }
        val request =
            CreatePlaceEventRequest(
                placeName = place.name,
                lat = place.point.lat,
                lon = place.point.lon,
                title = draft.title.trim(),
                startsAt = startsAt.toString(),
                note = null,
            )
        return withContext(ioDispatcher) { repository.createPlaceEvent(request) }
    }
}
