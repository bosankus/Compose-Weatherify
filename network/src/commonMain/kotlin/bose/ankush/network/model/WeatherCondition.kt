package bose.ankush.network.model

import kotlinx.serialization.Serializable

/**
 * Domain model for weather condition
 */
@Serializable
data class WeatherCondition(
    val description: String? = null,
    val icon: String? = null,
    val id: Int,
    val main: String? = null
)
