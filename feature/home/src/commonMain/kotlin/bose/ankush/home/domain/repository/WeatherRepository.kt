package bose.ankush.home.domain.repository

import bose.ankush.home.domain.location.ActiveCoordinates
import bose.ankush.home.domain.model.RefreshOutcome
import bose.ankush.home.domain.model.WeatherSnapshot
import kotlinx.coroutines.flow.Flow

internal interface WeatherRepository {
    /** The one saved snapshot, labelled with its own place. Emits again after every [refresh] that saves. */
    val snapshot: Flow<WeatherSnapshot?>

    /**
     * Fetches /weather for [place] and saves it with the place's point and name, unless the saved
     * row is [place]'s, under an hour old, and [forceRefresh] is false. A network failure keeps
     * the saved row and is returned, not thrown.
     */
    suspend fun refresh(
        place: ActiveCoordinates,
        forceRefresh: Boolean = false,
    ): Result<RefreshOutcome>

    suspend fun clearAllData()
}
