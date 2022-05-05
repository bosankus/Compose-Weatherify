package bose.ankush.weatherify.domain.use_case.get_weather_forecasts

import bose.ankush.weatherify.R
import bose.ankush.weatherify.common.Extension.getForecastListForNext4Days
import bose.ankush.weatherify.common.ResultData
import bose.ankush.weatherify.common.UiText
import bose.ankush.weatherify.common.errorResponse
import bose.ankush.weatherify.domain.model.AvgForecast
import bose.ankush.weatherify.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class GetNextFourDaysWeatherForecast @Inject constructor(
    private val repository: WeatherRepository
) {

    operator fun invoke(): Flow<ResultData<List<AvgForecast>>> = flow {
        try {
            emit(ResultData.Loading)
            val avgForecastList: List<AvgForecast>? =
                repository.getWeatherForecastList().list?.getForecastListForNext4Days()
            if (!avgForecastList.isNullOrEmpty()) emit(ResultData.Success(avgForecastList))
            else emit(ResultData.Failed(UiText.StringResource(R.string.empty_list)))
        } catch (e: HttpException) {
            val message = errorResponse(e.code())
            emit(ResultData.Failed(message))
        } catch (e: IOException) {
            emit(ResultData.Failed(UiText.DynamicText(e.message.toString())))
        }
    }
}