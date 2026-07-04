package bose.ankush.finder.data.mapper

import bose.ankush.finder.domain.model.Location
import bose.ankush.finder.domain.model.LocationSuggestion
import bose.ankush.network.model.PlaceSuggestion
import bose.ankush.network.model.SavedLocation

internal fun SavedLocation.toDomain(): Location =
    Location(
        id = id,
        name = name,
        lat = lat,
        lon = lon,
    )

internal fun PlaceSuggestion.toDomain(): LocationSuggestion =
    LocationSuggestion(
        name = name,
        city = city,
        state = state,
        country = country,
        latitude = latitude.toDoubleOrNull() ?: 0.0,
        longitude = longitude.toDoubleOrNull() ?: 0.0,
    )
