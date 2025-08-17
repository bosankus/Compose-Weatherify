package bose.ankush.network.auth.api

import bose.ankush.network.auth.model.AuthResponse
import bose.ankush.network.auth.model.LoginRequest
import bose.ankush.network.auth.model.LogoutResponse
import bose.ankush.network.auth.model.RefreshTokenRequest
import bose.ankush.network.auth.model.RegisterRequest

/**
 * API service interface for authentication operations
 */
interface AuthApiService {
    /**
     * Login with email and password
     * @param request LoginRequest containing email and password
     * @return AuthResponse with JWT token
     */
    suspend fun login(request: LoginRequest): AuthResponse

    /**
     * Register with email and password
     * @param request RegisterRequest containing email and password
     * @return AuthResponse with JWT token
     */
    suspend fun register(request: RegisterRequest): AuthResponse

    /**
     * Refresh JWT token
     * @param request RefreshTokenRequest containing the expired token
     * @return AuthResponse with new JWT token
     */
    suspend fun refreshToken(request: RefreshTokenRequest): AuthResponse

    /**
     * Logout the current user
     * @return LogoutResponse indicating success or failure
     */
    suspend fun logout(): LogoutResponse
}