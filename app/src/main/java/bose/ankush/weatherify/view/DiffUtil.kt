package bose.ankush.weatherify.view

import androidx.recyclerview.widget.DiffUtil
import bose.ankush.weatherify.model.model.AvgForecast

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/

class DiffUtil : DiffUtil.ItemCallback<AvgForecast>() {
    override fun areItemsTheSame(
        oldItem: AvgForecast,
        newItem: AvgForecast,
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: AvgForecast,
        newItem: AvgForecast,
    ): Boolean {
        return oldItem.hashCode() == newItem.hashCode()
    }

}