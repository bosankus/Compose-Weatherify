package bose.ankush.weatherify.common

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import bose.ankush.weatherify.R
import bose.ankush.weatherify.domain.model.AvgForecast
import bose.ankush.weatherify.domain.model.Weather
import bose.ankush.weatherify.common.Extension.toCelsius
import bose.ankush.weatherify.presentation.home.ForecastAdapter
import com.bumptech.glide.Glide

/**Created by
Author: Ankush Bose
Date: 06,May,2021
 **/

@BindingAdapter("isCurrentTempLoading", "isWeatherForecastLoading")
fun View.loadingVisibility(currentTempState: ResultData<*>, weatherForecastState: ResultData<*>) {
    if (currentTempState is ResultData.Loading || weatherForecastState is ResultData.Loading)
        this.visibility = View.VISIBLE
    else this.visibility = View.GONE
}


@BindingAdapter("isCurrentTempFailed", "isWeatherForecastFailed")
fun View.errorVisibility(currentTempState: ResultData<*>, weatherForecastState: ResultData<*>) {
    if (currentTempState is ResultData.Failed || weatherForecastState is ResultData.Failed)
        this.visibility = View.VISIBLE
    else this.visibility = View.GONE
}


@BindingAdapter("isCurrentTempSuccess", "isWeatherForecastSuccess")
fun View.weatherVisibility(currentTempState: ResultData<*>, weatherForecastState: ResultData<*>) {
    if (currentTempState is ResultData.Success || weatherForecastState is ResultData.Success)
        this.visibility = View.VISIBLE
    else this.visibility = View.GONE
}


@BindingAdapter("setTempInCelsius")
fun TextView.setTempInCelsius(value: ResultData<*>) {
    text =
        if (value is ResultData.Success<*> && value.data is Weather)
            resources.getString(R.string.celsius, "${value.data.main?.temp?.toCelsius()}")
        else resources.getString(R.string.celsius, "0")
}

@BindingAdapter("setHumidity")
fun TextView.setHumidity(value: ResultData<*>) {
    text =
        if (value is ResultData.Success<*> && value.data is Weather)
            value.data.main?.humidity.toString()

        else "0"
}


@BindingAdapter("setTempInCelsius")
fun TextView.setTempInCelsius(value: String?) {
    text = resources.getString(R.string.celsius, value)
}


@BindingAdapter("setCurrentCity")
fun TextView.setCurrentCity(value: ResultData<*>) {
    text =
        if (value is ResultData.Success<*> && value.data is Weather)
            value.data.name
        else "..."
}


@BindingAdapter("errorFetchingTemperature", "errorFetchingForecast")
fun TextView.setErrorText(tempErrMsg: ResultData<*>, forecastErrMsg: ResultData<*>) {
    text = when {
        tempErrMsg is ResultData.Failed -> tempErrMsg.message?.asString(this.context)
        forecastErrMsg is ResultData.Failed -> forecastErrMsg.message?.asString(this.context)
        else -> String()
    }
}


@BindingAdapter("setWeatherIcon")
fun ImageView.setIcon(result: ResultData<*>) {
    if (result is ResultData.Success && result.data is Weather) {
        result.data.weather?.get(0)?.icon?.let {
            val iconUrl = "https://openweathermap.org/img/wn/${it}@2x.png"
            Glide.with(this.context)
                .load(iconUrl)
                .into(this)
        }
    }
}


@BindingAdapter("weatherForecastList")
fun RecyclerView.setForecastData(response: ResultData<List<AvgForecast>>) {
    val forecastAdapter = ForecastAdapter()
    this.adapter = forecastAdapter
    if (response is ResultData.Success<List<AvgForecast>>) {
        val forecastList = response.data
        forecastAdapter.submitList(forecastList)
    } else forecastAdapter.submitList(emptyList())
}