package bose.ankush.network.auth.repository

import bose.ankush.network.auth.model.AuthResponse
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for authentication operations
 */
interface AuthRepository {
    /**
     * Login with email and password
     * @param email User's email
     * @param password User's password
     * @return Flow of AuthResponse
     */
    suspend fun login(email: String, password: String): AuthResponse

    /**
     * Register with email and password and additional device information
     * @param email User's email
     * @param password User's password
     * @param timestampOfRegistration UTC timestamp of registration
     * @param deviceModel Device model (e.g., "Pixel 7 Pro")
     * @param operatingSystem Operating system (e.g., "Android")
     * @param osVersion Operating system version (e.g., "14")
     * @param appVersion App version
     * @param ipAddress Client's IP address (if obtainable)
     * @param registrationSource Registration source (e.g., "Android App")
     * @return AuthResponse
     */
    suspend fun register(
        email: String,
        password: String,
        timestampOfRegistration: String? = null,
        deviceModel: String? = null,
        operatingSystem: String? = null,
        osVersion: String? = null,
        appVersion: String? = null,
        ipAddress: String? = null,
        registrationSource: String? = null,
        firebaseToken: String? = null
    ): AuthResponse

    /**
     * Check if user is logged in
     * @return Flow of Boolean indicating login status
     */
    fun isLoggedIn(): Flow<Boolean>

    /**
     * Get the current JWT token
     * @return The JWT token or null if not logged in
     */
    suspend fun getToken(): String?

    /**
     * Refresh the JWT token
     * @return AuthResponse with new token
     */
    suspend fun refreshToken(): AuthResponse?

    /**
     * Logout the user
     */
    suspend fun logout(): Result<Unit>
}