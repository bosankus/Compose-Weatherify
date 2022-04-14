package bose.ankush.weatherify.data

import bose.ankush.weatherify.R
import bose.ankush.weatherify.data.model.AvgForecast
import bose.ankush.weatherify.data.model.CurrentTemperature
import bose.ankush.weatherify.dispatcher.DispatcherProvider
import bose.ankush.weatherify.util.Extension.getForecastListForNext4Days
import bose.ankush.weatherify.util.ResultData
import bose.ankush.weatherify.util.UiText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/

class WeatherRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val dispatcher: DispatcherProvider,
) : WeatherRepository {

    override fun getCurrentTemperature(): Flow<ResultData<CurrentTemperature>> = flow {
        emit(ResultData.Loading)
        val response = apiService.getCurrentTemperature()
        if (response.isSuccessful) emit(ResultData.Success(response.body()))
        else {
            val errorCode = response.code()
            emit(ResultData.Failed(errorResponse(errorCode)))
        }
    }
        .catch { e -> ResultData.Failed(UiText.DynamicText(e.message.toString())) }
        .flowOn(dispatcher.io)


    override fun getWeatherForecast(): Flow<ResultData<List<AvgForecast>>> = flow {
        emit(ResultData.Loading)
        val response = apiService.getWeatherForecast()
        if (response.isSuccessful) {
            val result = response.body()?.list?.getForecastListForNext4Days() ?: emptyList()
            emit(ResultData.Success(result))
        }
        else {
            val errorCode = response.code()
            emit(ResultData.Failed(errorResponse(errorCode)))
        }
    }
        .catch { e -> ResultData.Failed(UiText.DynamicText(e.message.toString())) }
        .flowOn(dispatcher.io)


    override fun errorResponse(errorCode: Int): UiText.StringResource {
        return when (errorCode) {
            401 -> UiText.StringResource(resId = R.string.unauthorised_access_txt)
            400, 404 -> UiText.StringResource(resId = R.string.city_error_txt)
            500 -> UiText.StringResource(resId = R.string.server_error_txt)
            else -> UiText.StringResource(resId = R.string.general_error_txt)
        }
    }


}


