package bose.ankush.network.auth.model

import kotlinx.serialization.Serializable

/**
 * Request model for login operation
 */
@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

/**
 * Request model for register operation
 */
@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val timestampOfRegistration: String? = null,
    val deviceModel: String? = null,
    val operatingSystem: String? = null,
    val osVersion: String? = null,
    val appVersion: String? = null,
    val ipAddress: String? = null,
    val registrationSource: String? = null,
    val firebaseToken: String? = null
)

/**
 * Request model for token refresh operation
 */
@Serializable
data class RefreshTokenRequest(
    val token: String
)

/**
 * Data class for authentication response data
 */
@Serializable
data class AuthData(
    val token: String? = null,
    val email: String? = null,
    val role: String? = null,
    val isActive: Boolean? = null,
    val isPremium: Boolean? = null
)

/**
 * Response model for authentication operations
 */
@Serializable
data class AuthResponse(
    val success: Boolean? = null,
    val status: Boolean = true,
    val message: String? = null,
    val data: AuthData? = null
) {
    fun isSuccess(): Boolean = success ?: status
}

@Serializable
data class LogoutErrorData(
    val errorType: String? = null,
    val errorMessage: String? = null,
    val errorClass: String? = null,
    val endpoint: String? = null
)

@Serializable
data class LogoutResponse(
    val message: String? = null,
    val data: LogoutErrorData? = null
)
