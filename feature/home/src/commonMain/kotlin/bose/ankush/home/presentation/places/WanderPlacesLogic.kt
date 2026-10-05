package bose.ankush.home.presentation.places

import bose.ankush.finder.domain.model.Location
import bose.ankush.finder.domain.model.LocationSuggestion
import bose.ankush.storage.model.LocationPreferences
import kotlin.math.abs
import kotlin.math.round

/** Pure rules behind the Wander saved places page and search sheet. */
internal object WanderPlacesLogic {
    const val MIN_QUERY_LENGTH = 2

    /** Same tolerance for "this saved place is the active one" and "already saved". */
    private const val SAME_PLACE_DEGREES = 0.0005
    private const val COORDINATE_SCALE = 10_000.0

    fun isSearchable(query: String): Boolean = query.trim().length >= MIN_QUERY_LENGTH

    /** The pinned override, or null while Home follows GPS. */
    fun activePlace(prefs: LocationPreferences): ActivePlace? {
        val lat = prefs.overrideLat
        val lon = prefs.overrideLon
        return if (prefs.isLocationOverridden && lat != null && lon != null) {
            ActivePlace(lat = lat, lon = lon, name = prefs.overrideLocationName)
        } else {
            null
        }
    }

    fun isActive(
        place: Location,
        active: ActivePlace?,
    ): Boolean = active != null && samePlace(place.lat, place.lon, active.lat, active.lon)

    fun findSaved(
        suggestion: LocationSuggestion,
        places: List<Location>,
    ): Location? = places.firstOrNull { samePlace(it.lat, it.lon, suggestion.latitude, suggestion.longitude) }

    /** Name stored with a saved place. Matches the old saved locations screen. */
    fun savedName(suggestion: LocationSuggestion): String =
        suggestion.name
            .trim()
            .ifBlank {
                suggestion.city.trim()
            }.ifBlank { formatCoordinates(suggestion.latitude, suggestion.longitude) }

    /** Second line of a search result: city, state, country without blanks or repeats of the name. */
    fun suggestionDetail(suggestion: LocationSuggestion): String {
        val name = savedName(suggestion)
        return listOf(suggestion.city, suggestion.state, suggestion.country)
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.equals(name, ignoreCase = true) }
            .distinct()
            .joinToString(", ")
    }

    /** One card per place. Empty ids fall back to coordinates and name, like the old list. */
    fun distinctPlaces(places: List<Location>): List<Location> = places.distinctBy(::placeKey)

    fun placeKey(place: Location): String = place.id.ifEmpty { "${place.lat}_${place.lon}_${place.name}" }

    fun formatCoordinates(
        lat: Double,
        lon: Double,
    ): String =
        "${round(lat * COORDINATE_SCALE) / COORDINATE_SCALE}, ${round(lon * COORDINATE_SCALE) / COORDINATE_SCALE}"

    private fun samePlace(
        lat: Double,
        lon: Double,
        otherLat: Double,
        otherLon: Double,
    ): Boolean = abs(lat - otherLat) < SAME_PLACE_DEGREES && abs(lon - otherLon) < SAME_PLACE_DEGREES
}
