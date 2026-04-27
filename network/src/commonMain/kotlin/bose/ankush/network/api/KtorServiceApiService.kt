package bose.ankush.network.api

import bose.ankush.network.model.ServiceListResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class KtorServiceApiService(
    private val httpClient: HttpClient,
    private val baseUrl: String
) : ServiceApiService {

    override suspend fun getServices(
        page: Int,
        pageSize: Int,
        search: String?
    ): Result<ServiceListResponse> {
        return try {
            val response = httpClient.get("$baseUrl/services/public") {
                parameter("page", page)
                parameter("pageSize", pageSize)
                if (!search.isNullOrBlank()) {
                    parameter("search", search)
                }
            }.body<ServiceListResponse>()

            if (response.success) {
                Result.success(response)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
