package bose.ankush.storage.model

data class LocationPreferences(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isLocationOverridden: Boolean = false,
    val overrideLat: Double? = null,
    val overrideLon: Double? = null,
    val overrideLocationName: String? = null,
)
