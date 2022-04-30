package bose.ankush.weatherify.domain.model


import com.google.gson.annotations.SerializedName

data class Weather(
    @SerializedName("cod")
    val cod: Int,
    @SerializedName("main")
    val main: Main?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("weather")
    val weather: List<Weather?>?,
) {
    data class Main(
        @SerializedName("temp")
        val temp: Double?,
        @SerializedName("humidity")
        val humidity: Double?,
    )
    data class Weather(
        @SerializedName("icon")
        val icon: String?,
    )
}