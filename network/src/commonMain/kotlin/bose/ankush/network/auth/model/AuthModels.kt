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
 * User role returned by the server on login/register/refresh.
 * Unknown values from the server coerce to null (requires coerceInputValues = true in JSON config).
 */
@Serializable
enum class UserRole {
    USER,
    ADMIN
}

/**
 * Data class for authentication response data.
 * Defaults allow this to be used for both success and error shapes
 * (e.g. TOKEN_NOT_EXPIRED only has errorCode, no token/email).
 */
@Serializable
data class AuthData(
    val token: String = "",
    val email: String = "",
    val role: UserRole? = null,
    val isActive: Boolean = false,
    val isPremium: Boolean = false,
    val premiumExpiresAt: String? = null,
    val errorCode: String? = null
)

/**
 * Response model for authentication operations
 */
@Serializable
data class AuthResponse(
    val success: Boolean? = null,
    val status: Boolean = false,
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
