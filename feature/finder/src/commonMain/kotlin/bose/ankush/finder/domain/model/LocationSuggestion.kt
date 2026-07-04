package bose.ankush.finder.domain.model

data class LocationSuggestion(
    val name: String,
    val city: String,
    val state: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
)
