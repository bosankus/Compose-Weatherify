package bose.ankush.network.repository

import bose.ankush.network.model.Service
import bose.ankush.network.model.toDomain

interface ServiceRepository {
    suspend fun getServices(
        page: Int = 1,
        pageSize: Int = 20,
        search: String? = null,
    ): Result<List<Service>>
}

class ServiceRepositoryImpl(
    private val api: bose.ankush.network.api.ServiceApiService,
) : ServiceRepository {
    override suspend fun getServices(
        page: Int,
        pageSize: Int,
        search: String?,
    ): Result<List<Service>> =
        try {
            val response = api.getServices(page, pageSize, search)
            response.fold(
                onSuccess = { data ->
                    val services = data.data.services.map { it.toDomain() }
                    Result.success(services)
                },
                onFailure = { error ->
                    logError("Service API Error", error)
                    Result.failure(error)
                },
            )
        } catch (e: Exception) {
            logError("Service Repository Error", e)
            Result.failure(e)
        }

    private fun logError(
        tag: String,
        error: Throwable,
    ) {
        // Log to Firebase or your analytics service
        // FirebaseCrashlytics.getInstance().recordException(error)
        println("$tag: ${error.message}")
    }
}
