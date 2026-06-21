package bose.ankush.network.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * Handles MongoDB Extended JSON ObjectId format {"$oid": "..."} and plain strings.
 */
private object ObjectIdAsStringSerializer : KSerializer<String> {
    override val descriptor = PrimitiveSerialDescriptor("ObjectId", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: String,
    ) = encoder.encodeString(value)

    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeString()
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonObject -> element["\$oid"]?.jsonPrimitive?.content ?: element.toString()
            is JsonPrimitive -> element.content
            else -> element.toString()
        }
    }
}

/**
 * A saved favourite location returned by GET /saved-places.
 */
@Serializable
data class SavedLocation(
    @SerialName("_id")
    @Serializable(with = ObjectIdAsStringSerializer::class)
    val id: String = "",
    val userEmail: String = "",
    val name: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val createdAt: String = "",
)

/**
 * Request body for POST /save-location.
 */
@Serializable
data class SaveLocationRequest(
    val name: String,
    val lat: Double,
    val lon: Double,
)

/**
 * Generic API envelope shared across location endpoints.
 */
@Serializable
data class ApiResponse<T>(
    val status: Boolean,
    val message: String,
    val data: T? = null,
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
    val longitude: String,
)
