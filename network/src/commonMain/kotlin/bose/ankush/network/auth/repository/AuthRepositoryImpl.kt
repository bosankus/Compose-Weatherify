package bose.ankush.network.auth.repository

import bose.ankush.network.auth.api.AuthApiService
import bose.ankush.network.auth.model.AuthResponse
import bose.ankush.network.auth.model.LoginRequest
import bose.ankush.network.auth.model.RefreshTokenRequest
import bose.ankush.network.auth.model.RegisterRequest
import bose.ankush.storage.api.TokenStorage
import kotlinx.coroutines.flow.Flow

/**
 * Implementation of AuthRepository
 */
class AuthRepositoryImpl(
    private val apiService: AuthApiService,
    private val tokenStorage: TokenStorage,
) : AuthRepository {
    override suspend fun login(
        email: String,
        password: String,
    ): AuthResponse {
        val request = LoginRequest(email = email, password = password)
        val response = apiService.login(request)

        // Save token on successful login
        val token = response.data?.token
        if (response.isSuccess() && !token.isNullOrBlank()) {
            tokenStorage.saveToken(token)
        }

        return response
    }

    override suspend fun register(
        email: String,
        password: String,
        timestampOfRegistration: String?,
        deviceModel: String?,
        operatingSystem: String?,
        osVersion: String?,
        appVersion: String?,
        registrationSource: String?,
        firebaseToken: String?,
    ): AuthResponse {
        val request =
            RegisterRequest(
                email = email,
                password = password,
                timestampOfRegistration = timestampOfRegistration,
                deviceModel = deviceModel,
                operatingSystem = operatingSystem,
                osVersion = osVersion,
                appVersion = appVersion,
                registrationSource = registrationSource,
                firebaseToken = firebaseToken,
            )
        val response = apiService.register(request)

        // Save token on successful registration
        val token = response.data?.token
        if (response.isSuccess() && !token.isNullOrBlank()) {
            tokenStorage.saveToken(token)
        }

        return response
    }

    override fun isLoggedIn(): Flow<Boolean> = tokenStorage.hasToken()

    override suspend fun getToken(): String? = tokenStorage.getToken()

    override suspend fun refreshToken(): AuthResponse? {
        val currentToken = tokenStorage.getToken() ?: return null

        val request = RefreshTokenRequest(token = currentToken)
        val response = apiService.refreshToken(request)

        // Save new token on successful refresh
        val token = response.data?.token
        if (response.isSuccess() && !token.isNullOrBlank()) {
            tokenStorage.saveToken(token)
        }

        return response
    }

    override suspend fun logout(): Result<Unit> =
        try {
            val response = apiService.logout()
            val isSuccess = response.data == null
            if (isSuccess) {
                tokenStorage.clearToken()
                Result.success(Unit)
            } else {
                val errorMsg = response.data.errorMessage
                val message =
                    if (!errorMsg.isNullOrBlank()) {
                        errorMsg
                    } else {
                        response.message ?: "Logout failed"
                    }
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
}
