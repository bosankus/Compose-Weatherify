package bose.ankush.weatherify.view


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import bose.ankush.weatherify.databinding.LayoutForecastItemBinding
import bose.ankush.weatherify.model.AvgForecast

/**Created by
Author: Ankush Bose
Date: 06,May,2021
 **/
class ForecastAdapter : ListAdapter<AvgForecast, ForecastViewHolder>(DiffUtil()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = LayoutForecastItemBinding.inflate(layoutInflater, parent, false)
        return ForecastViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int) {
        val forecastItem = getItem(position)
        holder.bind(forecastItem)
    }
}