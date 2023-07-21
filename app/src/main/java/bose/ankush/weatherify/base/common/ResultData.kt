package bose.ankush.weatherify.base.common

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/

sealed class ResultData<out T> {
    object DoNothing : ResultData<Nothing>()
    object Loading : ResultData<Nothing>()
    data class Success<out T>(val data: T? = null) : ResultData<T>()
    data class Failed(val message: UiText? = null) : ResultData<Nothing>()
}