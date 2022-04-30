package bose.ankush.weatherify.domain.model

data class Weather(
    val cod: Int,
    val temp: Double?,
    val humidity: Int?,
    val name: String?,
    val icon: String?,
)