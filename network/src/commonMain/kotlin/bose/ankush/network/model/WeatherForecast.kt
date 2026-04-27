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
        @SerialName("alerts")
        val alerts: List<Alert?>? = null,
        @SerialName("current")
        val current: Current?,
        @SerialName("daily")
        val daily: List<Daily?>?,
        @SerialName("hourly")
        val hourly: List<Hourly?>?,
        @SerialName("airQuality")
        val airQuality: AirQuality.Data? = null,
        @SerialName("entitlements")
        val entitlements: Entitlements? = null
    ) {
        @Serializable
        data class WeatherInfo(
            @SerialName("description")
            val description: String = "",
            @SerialName("icon")
            val icon: String = "",
            @SerialName("id")
            val id: Int = 0,
            @SerialName("main")
            val main: String = ""
        )

        @Serializable
        data class Alert(
            val description: String?,
            val end: Long?,
            val event: String?,
            @SerialName("senderName") val senderName: String?,
            val start: Long?,
        )

        @Serializable
        data class Current(
            @SerialName("clouds")
            val clouds: Int? = null,
            @SerialName("dt")
            val dt: Long? = null,
            @SerialName("feelsLike")
            val feelsLike: Double? = null,
            @SerialName("humidity")
            val humidity: Int? = null,
            @SerialName("pressure")
            val pressure: Int? = null,
            @SerialName("sunrise")
            val sunrise: Long? = null,
            @SerialName("sunset")
            val sunset: Long? = null,
            @SerialName("temp")
            val temp: Double? = null,
            @SerialName("uvi")
            val uvi: Double? = null,
            @SerialName("weather")
            val weather: List<WeatherInfo?>? = null,
            @SerialName("windGust")
            val windGust: Double? = null,
            @SerialName("windSpeed")
            val windSpeed: Double? = null
        )

        @Serializable
        data class Daily(
            @SerialName("clouds")
            val clouds: Int? = null,
            @SerialName("dewPoint")
            val dewPoint: Double? = null,
            @SerialName("dt")
            val dt: Long? = null,
            @SerialName("humidity")
            val humidity: Int? = null,
            @SerialName("pressure")
            val pressure: Int? = null,
            @SerialName("rain")
            val rain: Double? = null,
            @SerialName("summary")
            val summary: String? = null,
            @SerialName("sunrise")
            val sunrise: Long? = null,
            @SerialName("sunset")
            val sunset: Long? = null,
            @SerialName("temp")
            val temp: Temp? = null,
            @SerialName("uvi")
            val uvi: Double? = null,
            @SerialName("weather")
            val weather: List<WeatherInfo?>? = null,
            @SerialName("windGust")
            val windGust: Double? = null,
            @SerialName("windSpeed")
            val windSpeed: Double? = null
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
        }

        @Serializable
        data class Hourly(
            @SerialName("clouds")
            val clouds: Int? = null,
            @SerialName("dt")
            val dt: Long? = null,
            @SerialName("feelsLike")
            val feelsLike: Double? = null,
            @SerialName("humidity")
            val humidity: Int? = null,
            @SerialName("temp")
            val temp: Double? = null,
            @SerialName("weather")
            val weather: List<WeatherInfo?>? = null
        )

        @Serializable
        data class Entitlements(
            val hourlyIncluded: Boolean = false,
            val dailyIncluded: Boolean = false,
            val alertsIncluded: Boolean = false,
            val airQualityIncluded: Boolean = false,
            val upgradeRequired: Boolean = true
        )
    }
}
