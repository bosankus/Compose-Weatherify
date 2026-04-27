package bose.ankush.network.api

import bose.ankush.network.model.ServiceListResponse

interface ServiceApiService {
    suspend fun getServices(
        page: Int = 1,
        pageSize: Int = 20,
        search: String? = null
    ): Result<ServiceListResponse>
}
