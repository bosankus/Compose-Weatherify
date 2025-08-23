package bose.ankush.network.auth.repository

import bose.ankush.network.auth.api.AuthApiService
import bose.ankush.network.auth.model.AuthResponse
import bose.ankush.network.auth.model.LoginRequest
import bose.ankush.network.auth.model.RefreshTokenRequest
import bose.ankush.network.auth.model.RegisterRequest
import bose.ankush.network.auth.storage.TokenStorage
import kotlinx.coroutines.flow.Flow

/**
 * Implementation of AuthRepository
 */
class AuthRepositoryImpl(
    private val apiService: AuthApiService,
    private val tokenStorage: TokenStorage
) : AuthRepository {

    override suspend fun login(email: String, password: String): AuthResponse {
        val request = LoginRequest(email = email, password = password)
        val response = apiService.login(request)

        // Save token on successful login
        val token = response.data?.token
        if (response.isSuccess() && token != null && token.isNotBlank()) {
            tokenStorage.saveToken(token)

            // Verify token was saved correctly
            verifyTokenSaved(token)
        }

        return response
    }

    /**
     * Verifies that a token was correctly saved to the database
     * @param originalToken The token that was supposed to be saved
     */
    private suspend fun verifyTokenSaved(originalToken: String) {
        val savedToken = tokenStorage.getToken()
        if (savedToken != originalToken) {
            println("[DEBUG_LOG] Token verification failed: token mismatch")
        } else {
            println("[DEBUG_LOG] Token verification successful")
        }
    }

    override suspend fun register(
        email: String,
        password: String,
        timestampOfRegistration: String?,
        deviceModel: String?,
        operatingSystem: String?,
        osVersion: String?,
        appVersion: String?,
        ipAddress: String?,
        registrationSource: String?,
        firebaseToken: String?
    ): AuthResponse {
        val request = RegisterRequest(
            email = email,
            password = password,
            timestampOfRegistration = timestampOfRegistration,
            deviceModel = deviceModel,
            operatingSystem = operatingSystem,
            osVersion = osVersion,
            appVersion = appVersion,
            ipAddress = ipAddress,
            registrationSource = registrationSource,
            firebaseToken = firebaseToken
        )
        val response = apiService.register(request)

        // Save token on successful registration
        val token = response.data?.token
        if (response.isSuccess() && token != null && token.isNotBlank()) {
            tokenStorage.saveToken(token)

            // Verify token was saved correctly
            verifyTokenSaved(token)
        }

        return response
    }

    override fun isLoggedIn(): Flow<Boolean> {
        return tokenStorage.hasToken()
    }

    override suspend fun getToken(): String? {
        return tokenStorage.getToken()
    }

    override suspend fun refreshToken(): AuthResponse? {
        val currentToken = tokenStorage.getToken() ?: return null

        val request = RefreshTokenRequest(token = currentToken)
        val response = apiService.refreshToken(request)

        // Save new token on successful refresh
        val token = response.data?.token
        if (response.isSuccess() && token != null && token.isNotBlank()) {
            tokenStorage.saveToken(token)

            // Verify token was saved correctly
            verifyTokenSaved(token)
        }

        return response
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            val response = apiService.logout()
            val message = response.message ?: ""
            val isSuccess = response.data == null && (
                    message.contains("Logout successful", ignoreCase = true) ||
                            message.contains("Logged out successfully", ignoreCase = true)
                    )
            if (isSuccess) {
                tokenStorage.clearToken()
                Result.success(Unit)
            } else {
                val errorMsg = response.data?.errorMessage
                val message = if (!errorMsg.isNullOrBlank()) {
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
}