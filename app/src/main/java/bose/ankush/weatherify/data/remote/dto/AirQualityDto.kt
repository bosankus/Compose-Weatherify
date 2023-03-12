package bose.ankush.weatherify.data.remote.dto


import bose.ankush.weatherify.domain.model.AirQuality
import com.google.gson.annotations.SerializedName

data class AirQualityDto(
    @SerializedName("coord")
    val coord: Coord,
    @SerializedName("list")
    val data: List<AirData>
) {
    data class Coord(
        @SerializedName("lat")
        val lat: Double,
        @SerializedName("lon")
        val lon: Double
    )

    data class AirData(
        @SerializedName("components")
        val components: Components,
        @SerializedName("dt")
        val dt: Int,
        @SerializedName("main")
        val main: Main
    ) {
        data class Components(
            @SerializedName("co")
            val co: Double,
            @SerializedName("nh3")
            val nh3: Double,
            @SerializedName("no")
            val no: Double,
            @SerializedName("no2")
            val no2: Double,
            @SerializedName("o3")
            val o3: Double,
            @SerializedName("pm10")
            val pm10: Double,
            @SerializedName("pm2_5")
            val pm25: Double,
            @SerializedName("so2")
            val so2: Double
        )

        data class Main(
            @SerializedName("aqi")
            val aqi: Int
        )
    }
}

internal fun AirQualityDto.toAirQuality(): AirQuality = AirQuality(
    aqi = data[0].main.aqi,
    co = data[0].components.co,
    no2 = data[0].components.no2,
    o3 = data[0].components.o3,
    so2 = data[0].components.o3,
    pm10 = data[0].components.pm10,
    pm25 = data[0].components.pm25,
    coord = Pair(coord.lat, coord.lon)
)