package bose.ankush.home.domain.usecase

import bose.ankush.home.domain.location.ActiveCoordinates
import bose.ankush.home.domain.location.CoordinateResult
import bose.ankush.home.domain.location.Coordinates
import bose.ankush.home.domain.location.LocationClient
import bose.ankush.home.domain.location.LocationProblem
import bose.ankush.storage.api.LocationPreferencesStorage
import bose.ankush.storage.model.LocationPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal interface GetActiveCoordinates {
    /** Pinned place, else a fresh GPS fix (saved), else the saved spot. */
    suspend fun current(): CoordinateResult

    /** The stored spot only: no permission check, no GPS. */
    suspend fun lastKnown(): ActiveCoordinates?

    /** Emits when the pinned place changes (pinned, switched or cleared). Not on subscribe. */
    fun changes(): Flow<Unit>

    /** Un-pins the place and goes back to GPS. */
    suspend fun reset()
}

internal class GetActiveCoordinatesImpl(
    private val client: LocationClient,
    private val storage: LocationPreferencesStorage,
) : GetActiveCoordinates {

    override suspend fun current(): CoordinateResult {
        val pref = storage.getLocationPreferencesFlow().first()
        val savedLocation = pref.toActiveCoordinates()
        if (pref.isLocationOverridden) return CoordinateResult(savedLocation)
        if (!client.hasLocationPermission()) return CoordinateResult(
            savedLocation,
            LocationProblem.PermissionDenied
        )
        return client.getCurrentLocation()
            .fold(
                onSuccess = {
                    storage.saveLocationPreferences(it.latitude to it.longitude)
                    CoordinateResult(ActiveCoordinates(Coordinates(it.latitude, it.longitude)))
                },
                onFailure = { CoordinateResult(savedLocation, it.toProblem()) }
            )
    }

    override suspend fun lastKnown(): ActiveCoordinates? {
        return storage.getLocationPreferencesFlow().first().toActiveCoordinates()
    }

    override fun changes(): Flow<Unit> =
        storage.getLocationPreferencesFlow()
            .map { Triple(it.isLocationOverridden, it.overrideLat, it.overrideLon) }
            .distinctUntilChanged()
            .drop(1)
            .map { }

    override suspend fun reset() = storage.clearLocationOverride()

    private fun LocationPreferences.toActiveCoordinates(): ActiveCoordinates? {
        val pinned = isLocationOverridden && overrideLat != null && overrideLon != null
        val latitude = if (pinned) overrideLat else latitude
        val longitude = if (pinned) overrideLon else longitude
        return if (latitude != null && longitude != null) {
            ActiveCoordinates(
                coordinates = Coordinates(latitude, longitude),
                isOverridden = pinned,
                overrideName = overrideLocationName.takeIf { pinned }
            )
        } else {
            null
        }
    }

    private fun Throwable.toProblem(): LocationProblem {
        return if (this is LocationClient.LocationException && isGpsDisabled) {
            LocationProblem.GpsDisabled(this)
        } else {
            LocationProblem.Failed(this)
        }
    }
}