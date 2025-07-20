package bose.ankush.network.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherForecast(
    @SerialName("data")
    val `data`: Data?,
    @SerialName("message")
    val message: String?,
    @SerialName("status")
    val status: Boolean?
) {
    @Serializable
    data class Data(
        @SerialName("current")
        val current: Current?,
        @SerialName("daily")
        val daily: List<Daily?>?,
        @SerialName("hourly")
        val hourly: List<Hourly?>?
    ) {
        @Serializable
        data class Current(
            @SerialName("clouds")
            val clouds: Int?,
            @SerialName("dt")
            val dt: Int?,
            @SerialName("feels_like")
            val feelsLike: Double?,
            @SerialName("humidity")
            val humidity: Int?,
            @SerialName("pressure")
            val pressure: Int?,
            @SerialName("sunrise")
            val sunrise: Int?,
            @SerialName("sunset")
            val sunset: Int?,
            @SerialName("temp")
            val temp: Double?,
            @SerialName("uvi")
            val uvi: Double?,
            @SerialName("weather")
            val weather: List<Weather?>?,
            @SerialName("wind_gust")
            val windGust: Double?,
            @SerialName("wind_speed")
            val windSpeed: Double?
        ) {
            @Serializable
            data class Weather(
                @SerialName("description")
                val description: String?,
                @SerialName("icon")
                val icon: String?,
                @SerialName("id")
                val id: Int?,
                @SerialName("main")
                val main: String?
            )
        }

        @Serializable
        data class Daily(
            @SerialName("clouds")
            val clouds: Int?,
            @SerialName("dew_point")
            val dewPoint: Double?,
            @SerialName("dt")
            val dt: Int?,
            @SerialName("humidity")
            val humidity: Int?,
            @SerialName("pressure")
            val pressure: Int?,
            @SerialName("rain")
            val rain: Double? = null,
            @SerialName("summary")
            val summary: String?,
            @SerialName("sunrise")
            val sunrise: Int?,
            @SerialName("sunset")
            val sunset: Int?,
            @SerialName("temp")
            val temp: Temp?,
            @SerialName("uvi")
            val uvi: Double?,
            @SerialName("weather")
            val weather: List<Weather?>?,
            @SerialName("wind_gust")
            val windGust: Double?,
            @SerialName("wind_speed")
            val windSpeed: Double?
        ) {
            @Serializable
            data class Temp(
                @SerialName("day")
                val day: Double?,
                @SerialName("eve")
                val eve: Double?,
                @SerialName("max")
                val max: Double?,
                @SerialName("min")
                val min: Double?,
                @SerialName("morn")
                val morn: Double?,
                @SerialName("night")
                val night: Double?
            )

            @Serializable
            data class Weather(
                @SerialName("description")
                val description: String?,
                @SerialName("icon")
                val icon: String?,
                @SerialName("id")
                val id: Int?,
                @SerialName("main")
                val main: String?
            )
        }

        @Serializable
        data class Hourly(
            @SerialName("clouds")
            val clouds: Int?,
            @SerialName("dt")
            val dt: Int?,
            @SerialName("feels_like")
            val feelsLike: Double?,
            @SerialName("humidity")
            val humidity: Int?,
            @SerialName("temp")
            val temp: Double?,
            @SerialName("weather")
            val weather: List<Weather?>?
        ) {
            @Serializable
            data class Weather(
                @SerialName("description")
                val description: String?,
                @SerialName("icon")
                val icon: String?,
                @SerialName("id")
                val id: Int?,
                @SerialName("main")
                val main: String?
            )
        }
    }
}
