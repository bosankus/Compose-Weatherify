package bose.ankush.weatherify.domain.use_case.get_cities

import bose.ankush.weatherify.R
import bose.ankush.weatherify.common.ResultData
import bose.ankush.weatherify.common.UiText
import bose.ankush.weatherify.data.remote.dto.toCityName
import bose.ankush.weatherify.domain.model.CityName
import bose.ankush.weatherify.domain.repository.CityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetCityNames @Inject constructor(
    private val repository: CityRepository
) {
    operator fun invoke(): Flow<ResultData<List<CityName>>> = flow {
        try {
            emit(ResultData.Loading)
            val cityNames = repository.getCityNames().map { it.toCityName() }
            if (cityNames.isNotEmpty()) emit(ResultData.Success(cityNames))
            else emit(ResultData.Failed(UiText.StringResource(resId = R.string.empty_city_list_error_txt)))
        } catch (e: Exception) {
            emit(ResultData.Failed(UiText.StringResource(resId = R.string.general_error_txt)))
        }
    }
}