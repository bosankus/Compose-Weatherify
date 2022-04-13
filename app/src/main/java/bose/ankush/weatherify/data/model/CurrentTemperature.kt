package bose.ankush.weatherify.data.model


import com.google.gson.annotations.SerializedName

data class CurrentTemperature(
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
    )

    data class Weather(
        @SerializedName("icon")
        val icon: String?,
    )
}