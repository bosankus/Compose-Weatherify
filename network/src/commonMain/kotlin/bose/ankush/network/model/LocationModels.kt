package bose.ankush.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A saved favourite location returned by GET /saved-places.
 */
@Serializable
data class SavedLocation(
    val id: String,
    val userEmail: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val createdAt: String
)

/**
 * Request body for POST /save-location.
 */
@Serializable
data class SaveLocationRequest(
    val name: String,
    val lat: Double,
    val lon: Double
)

/**
 * Generic API envelope shared across location endpoints.
 */
@Serializable
data class ApiResponse<T>(
    val status: Boolean,
    val message: String,
    val data: T? = null
)

/**
 * A place suggestion returned by GET /search-place.
 */
@Serializable
data class PlaceSuggestion(
    val name: String,
    val city: String,
    val state: String,
    val country: String,
    @SerialName("lat")
    val latitude: String,
    @SerialName("lon")
    val longitude: String
)
