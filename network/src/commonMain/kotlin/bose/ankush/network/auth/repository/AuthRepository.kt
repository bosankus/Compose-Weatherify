package bose.ankush.network.auth.repository

import bose.ankush.network.auth.model.AuthResponse
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(
        email: String,
        password: String,
    ): AuthResponse

    suspend fun register(
        email: String,
        password: String,
        timestampOfRegistration: String? = null,
        deviceModel: String? = null,
        operatingSystem: String? = null,
        osVersion: String? = null,
        appVersion: String? = null,
        registrationSource: String? = null,
        firebaseToken: String? = null,
    ): AuthResponse

    fun isLoggedIn(): Flow<Boolean>

    suspend fun getToken(): String?

    suspend fun refreshToken(): AuthResponse?

    suspend fun logout(): Result<Unit>
}
