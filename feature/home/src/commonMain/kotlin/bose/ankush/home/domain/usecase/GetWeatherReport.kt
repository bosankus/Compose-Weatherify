package bose.ankush.home.domain.usecase

import bose.ankush.home.domain.model.WeatherForecast
import bose.ankush.home.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

/**
 * Room is the source of truth. [invoke] only reads the cache.
 * [observeWithRefresh] still emits Room rows immediately and runs one network
 * refresh in parallel when collection starts.
 */
internal class GetWeatherReport(
    private val repository: WeatherRepository,
) {
    operator fun invoke(location: Pair<Double, Double>): Flow<WeatherForecast?> = repository.getWeatherReport(location)

    /**
     * The saved forecast, without waiting for saved or GPS coordinates. Room keeps one
     * global forecast row and ignores the coordinates, so first paint can read it directly.
     */
    fun cached(): Flow<WeatherForecast?> = repository.getWeatherReport(CACHE_PROBE_LOCATION)

    /**
     * Emits cached rows as soon as Room has them. Starts a single [refresh] for
     * this collection; Room pushes network writes afterward. Cancelling the
     * collector cancels the in-flight refresh.
     */
    fun observeWithRefresh(
        location: Pair<Double, Double>,
        refresh: suspend () -> Unit,
        onRefreshStart: () -> Unit = {},
        onRefreshEnd: () -> Unit = {},
    ): Flow<WeatherForecast?> =
        channelFlow {
            val refreshJob =
                launch {
                    onRefreshStart()
                    try {
                        refresh()
                    } finally {
                        onRefreshEnd()
                    }
                }
            try {
                repository.getWeatherReport(location).collect { send(it) }
            } finally {
                refreshJob.cancel()
            }
        }

    private companion object {
        /** Unused by the single-row Room table. Only satisfies the repository signature. */
        val CACHE_PROBE_LOCATION = 0.0 to 0.0
    }
}
