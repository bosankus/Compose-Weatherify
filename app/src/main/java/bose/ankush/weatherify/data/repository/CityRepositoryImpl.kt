package bose.ankush.weatherify.data.repository

import android.content.Context
import bose.ankush.weatherify.data.remote.dto.CityDto
import bose.ankush.weatherify.domain.repository.CityRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject

class CityRepositoryImpl @Inject constructor(
    private val context: Context
) : CityRepository {

    override fun getCityNames(): List<CityDto> {
        val jsonString: String = context.assets.open("city_names.json")
            .bufferedReader()
            .use { it.readText() }

        val listCountryType = object : TypeToken<List<CityDto>>() {}.type
        return Gson().fromJson(jsonString, listCountryType)
    }

}

