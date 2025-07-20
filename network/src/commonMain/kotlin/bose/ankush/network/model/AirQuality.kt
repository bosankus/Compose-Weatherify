package bose.ankush.network.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AirQuality(
    @SerialName("data")
    val `data`: Data?,
    @SerialName("message")
    val message: String?,
    @SerialName("status")
    val status: Boolean?
) {
    @Serializable
    data class Data(
        @SerialName("list")
        val list: List<Item9?>?
    ) {
        @Serializable
        data class Item9(
            @SerialName("components")
            val components: Components?,
            @SerialName("dt")
            val dt: Int?,
            @SerialName("main")
            val main: Main?
        ) {
            @Serializable
            data class Components(
                @SerialName("co")
                val co: Double?,
                @SerialName("nh3")
                val nh3: Double?,
                @SerialName("no")
                val no: Double?,
                @SerialName("no2")
                val no2: Double?,
                @SerialName("o3")
                val o3: Double?,
                @SerialName("pm10")
                val pm10: Double?,
                @SerialName("pm2_5")
                val pm25: Double?,
                @SerialName("so2")
                val so2: Double?
            )

            @Serializable
            data class Main(
                @SerialName("aqi")
                val aqi: Int?
            )
        }
    }

    // Helper properties to maintain backward compatibility
    val aqi: Int
        get() = data?.list?.firstOrNull()?.main?.aqi ?: 0
    val co: Double
        get() = data?.list?.firstOrNull()?.components?.co ?: 0.0
    val no2: Double
        get() = data?.list?.firstOrNull()?.components?.no2 ?: 0.0
    val o3: Double
        get() = data?.list?.firstOrNull()?.components?.o3 ?: 0.0
    val so2: Double
        get() = data?.list?.firstOrNull()?.components?.so2 ?: 0.0
    val pm10: Double
        get() = data?.list?.firstOrNull()?.components?.pm10 ?: 0.0
    val pm25: Double
        get() = data?.list?.firstOrNull()?.components?.pm25 ?: 0.0
}
