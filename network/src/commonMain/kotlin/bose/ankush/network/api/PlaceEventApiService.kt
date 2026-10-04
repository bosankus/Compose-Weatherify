package bose.ankush.network.api

import bose.ankush.network.model.ApiResponse
import bose.ankush.network.model.CreatePlaceEventRequest
import bose.ankush.network.model.PlaceEvent

/** Place events. The JWT is attached by [bose.ankush.network.auth.interceptor.authorizedRequest]. */
interface PlaceEventApiService {
    /** GET /place-events?lat=&lon= */
    suspend fun getPlaceEvents(
        lat: Double,
        lon: Double,
    ): ApiResponse<List<PlaceEvent>>

    /** POST /place-events */
    suspend fun createPlaceEvent(request: CreatePlaceEventRequest): ApiResponse<PlaceEvent?>
}
