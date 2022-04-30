package bose.ankush.weatherify.presentation.home

import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import bose.ankush.weatherify.databinding.LayoutForecastItemBinding
import bose.ankush.weatherify.domain.model.AvgForecast

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/

class ForecastViewHolder(private val binding: LayoutForecastItemBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(forecastItem: AvgForecast) {
        binding.apply {
            forecast = forecastItem
            executePendingBindings()

            layoutForecastClContainer.setOnClickListener {
                val action = HomeFragmentDirections.actionHomeFragmentToDetailsFragment()
                it?.findNavController()?.navigate(action)
            }
        }
    }
}