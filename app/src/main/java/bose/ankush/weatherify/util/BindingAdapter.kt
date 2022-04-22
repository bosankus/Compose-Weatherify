package bose.ankush.weatherify.util

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import bose.ankush.weatherify.R
import bose.ankush.weatherify.model.model.AvgForecast
import bose.ankush.weatherify.model.model.CurrentTemperature
import bose.ankush.weatherify.util.Extension.toCelsius
import bose.ankush.weatherify.view.ForecastAdapter
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
        if (value is ResultData.Success<*> && value.data is CurrentTemperature)
            resources.getString(R.string.celsius, "${value.data.main?.temp?.toCelsius()}")
        else resources.getString(R.string.celsius, "--")
}


@BindingAdapter("setTempInCelsius")
fun TextView.setTempInCelsius(value: String?) {
    text = resources.getString(R.string.celsius, value)
}


@BindingAdapter("setCurrentCity")
fun TextView.setCurrentCity(value: ResultData<*>) {
    text =
        if (value is ResultData.Success<*> && value.data is CurrentTemperature)
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
    if (result is ResultData.Success && result.data is CurrentTemperature) {
        result.data.weather?.get(0)?.icon?.let {
            val iconUrl = "https://openweathermap.org/img/w/${it}.png"
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