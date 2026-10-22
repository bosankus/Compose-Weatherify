package bose.ankush.weatherify.automotive

import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Fetches real parking lots from OpenStreetMap through the public Overpass API.
 * Free, no API key. Please keep requests light: it is a shared community server.
 *
 * Later this can be replaced by a repository from your shared KMP module. The
 * screens only depend on [nearby], so nothing else has to change.
 */
class ParkingRepository {
    /** Parking lots within [radiusMeters] of the given point, nearest first. */
    suspend fun nearby(
        lat: Double,
        lng: Double,
        radiusMeters: Int = 1500,
    ): List<ParkingSpot> =
        withContext(Dispatchers.IO) {
            val query =
                """[out:json][timeout:20];nwr["amenity"="parking"](around:$radiusMeters,$lat,$lng);out center 60;"""
            val url = URL("$ENDPOINT?data=${URLEncoder.encode(query, "UTF-8")}")

            val connection =
                (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 25_000
                    requestMethod = "GET"
                    // Overpass asks every client to identify itself.
                    setRequestProperty("User-Agent", "WeatherifyAuto/1.0 (learning project)")
                }
            try {
                val code = connection.responseCode
                if (code != HttpURLConnection.HTTP_OK) {
                    throw IOException("Parking service returned HTTP $code")
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                parse(body, lat, lng)
            } finally {
                connection.disconnect()
            }
        }

    private fun parse(
        json: String,
        fromLat: Double,
        fromLng: Double,
    ): List<ParkingSpot> {
        val elements = JSONObject(json).optJSONArray("elements") ?: return emptyList()
        return (0 until elements.length())
            .mapNotNull { i -> toSpot(elements.getJSONObject(i), fromLat, fromLng) }
            .sortedBy { it.distanceKm }
    }

    /** One Overpass element as a spot, or null when it is untagged, private, or has no position. */
    private fun toSpot(
        element: JSONObject,
        fromLat: Double,
        fromLng: Double,
    ): ParkingSpot? {
        val tags =
            element
                .optJSONObject("tags")
                // Skip private lots: not useful to a driver looking for parking.
                ?.takeUnless { it.optString("access") == "private" }
                ?: return null
        return element.position()?.let { (lat, lng) ->
            val results = FloatArray(1)
            Location.distanceBetween(fromLat, fromLng, lat, lng, results)
            ParkingSpot(
                name = tags.optString("name").ifBlank { "Parking lot" },
                distanceKm = results[0] / 1000.0,
                capacity = tags.optString("capacity").toIntOrNull(),
                isPaid =
                    when (tags.optString("fee")) {
                        "yes" -> true
                        "no" -> false
                        else -> null
                    },
                lat = lat,
                lng = lng,
            )
        }
    }

    /** Nodes carry lat/lon directly. Ways and relations carry a "center". */
    private fun JSONObject.position(): Pair<Double, Double>? {
        val center = optJSONObject("center")
        return when {
            has("lat") && has("lon") -> getDouble("lat") to getDouble("lon")
            center != null -> center.getDouble("lat") to center.getDouble("lon")
            else -> null
        }
    }

    private companion object {
        const val ENDPOINT = "https://overpass-api.de/api/interpreter"
    }
}
