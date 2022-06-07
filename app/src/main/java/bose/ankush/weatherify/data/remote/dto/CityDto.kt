package bose.ankush.weatherify.data.remote.dto

import bose.ankush.weatherify.domain.model.CityName

data class CityDto(
    val id: String? = "",
    val name: String = "",
    val state: String? = ""
)

fun CityDto.toCityName(): CityName {
    return CityName(
        name = name
    )
}
