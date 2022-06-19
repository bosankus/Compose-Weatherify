package bose.ankush.weatherify.data.remote.dto


import bose.ankush.weatherify.domain.model.AirQuality
import com.google.gson.annotations.SerializedName

data class AirQualityDto(
    @SerializedName("city_name")
    val cityName: String,
    @SerializedName("country_code")
    val countryCode: String,
    @SerializedName("data")
    val `data`: List<Data>,
    @SerializedName("lat")
    val lat: Double,
    @SerializedName("lon")
    val lon: Double,
    @SerializedName("state_code")
    val stateCode: String,
    @SerializedName("timezone")
    val timezone: String
) {
    data class Data(
        @SerializedName("aqi")
        val aqi: Int,
        @SerializedName("co")
        val co: Int,
        @SerializedName("mold_level")
        val moldLevel: Int,
        @SerializedName("no2")
        val no2: Double,
        @SerializedName("o3")
        val o3: Double,
        @SerializedName("pm10")
        val pm10: Double,
        @SerializedName("pm25")
        val pm25: Double,
        @SerializedName("pollen_level_grass")
        val pollenLevelGrass: Int,
        @SerializedName("pollen_level_tree")
        val pollenLevelTree: Int,
        @SerializedName("pollen_level_weed")
        val pollenLevelWeed: Int,
        @SerializedName("predominant_pollen_type")
        val predominantPollenType: String,
        @SerializedName("so2")
        val so2: Double
    )
}

fun AirQualityDto.toAirQuality(): AirQuality = AirQuality(
    aqi = data[0].aqi,
    co = data[0].co,
    no2 = data[0].no2,
    o3 = data[0].o3,
    so2 = data[0].so2,
    pm10 = data[0].pm10,
    pm25 = data[0].pm25
)