package bose.ankush.network.auth.api

import bose.ankush.network.auth.model.AuthResponse
import bose.ankush.network.auth.model.LoginRequest
import bose.ankush.network.auth.model.LogoutResponse
import bose.ankush.network.auth.model.RefreshTokenRequest
import bose.ankush.network.auth.model.RegisterRequest

interface AuthApiService {
    suspend fun login(request: LoginRequest): AuthResponse

    suspend fun register(request: RegisterRequest): AuthResponse

    suspend fun refreshToken(request: RefreshTokenRequest): AuthResponse

    suspend fun logout(): LogoutResponse
}
