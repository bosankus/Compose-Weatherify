package bose.ankush.weatherify.domain.model

data class Weather(
    val cod: Int = 0,
    val temp: Double? = 0.0,
    val wind: Double? = 0.0,
    val windAngle: Int? = 0,
    val humidity: Int? = 0,
    val name: String? = "",
    val icon: String? = "",
    val description: String? = ""
)