package bose.ankush.weatherify.domain.use_case.get_weather_reports

import bose.ankush.weatherify.common.ResultData
import bose.ankush.weatherify.common.UiText
import bose.ankush.weatherify.common.errorResponse
import bose.ankush.weatherify.data.remote.dto.toWeather
import bose.ankush.weatherify.domain.model.Weather
import bose.ankush.weatherify.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class GetTodaysWeatherReport @Inject constructor(
    private val repository: WeatherRepository
) {
    operator fun invoke(): Flow<ResultData<Weather>> = flow {
        try {
            emit(ResultData.Loading)
            val weatherReport: Weather = repository.getTodaysWeatherReport().toWeather()
            emit(ResultData.Success(weatherReport))
        } catch (e: HttpException) {
            val message = errorResponse(e.code())
            emit(ResultData.Failed(message))
        } catch (e: IOException) {
            emit(ResultData.Failed(UiText.DynamicText(e.message.toString())))
        }
    }
}