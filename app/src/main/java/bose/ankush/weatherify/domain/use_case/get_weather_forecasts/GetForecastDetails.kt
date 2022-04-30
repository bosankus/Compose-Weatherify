package bose.ankush.weatherify.domain.use_case.get_weather_forecasts

import bose.ankush.weatherify.R
import bose.ankush.weatherify.common.ResultData
import bose.ankush.weatherify.common.UiText
import bose.ankush.weatherify.common.errorResponse
import bose.ankush.weatherify.data.remote.dto.ForecastDto
import bose.ankush.weatherify.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class GetForecastDetails @Inject constructor(
    private val repository: WeatherRepository
) {
    operator fun invoke(): Flow<ResultData<List<ForecastDto.ForecastList>>> = flow {
        try {
            emit(ResultData.Loading)
            val forecastList: List<ForecastDto.ForecastList>? =
                repository.getWeatherForecastList().list
            if (!forecastList.isNullOrEmpty()) emit(ResultData.Success(forecastList))
            else emit(ResultData.Failed(UiText.StringResource(R.string.empty_list)))
            emit(ResultData.Success(forecastList))
        } catch (e: HttpException) {
            val message = errorResponse(e.code())
            emit(ResultData.Failed(message))
        } catch (e: IOException) {
            emit(ResultData.Failed(UiText.DynamicText(e.message.toString())))
        }
    }
}