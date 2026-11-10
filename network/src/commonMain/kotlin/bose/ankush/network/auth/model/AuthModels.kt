package bose.ankush.network.auth.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val timestampOfRegistration: String? = null,
    val deviceModel: String? = null,
    val operatingSystem: String? = null,
    val osVersion: String? = null,
    val appVersion: String? = null,
    val registrationSource: String? = null,
    val firebaseToken: String? = null,
)

/**
 * [token] is the current access token. [refreshToken] is sent only once the server has issued one:
 * it is never encoded as null, so a server that doesn't know the field never sees it.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class RefreshTokenRequest(
    val token: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val refreshToken: String? = null,
)

/**
 * Data class for authentication response data.
 * Defaults allow this to be used for both success and error shapes
 * (e.g. TOKEN_NOT_EXPIRED only has errorCode, no token/email).
 */
@Serializable
data class AuthData(
    val token: String = "",
    // Issued alongside [token] by a server that rotates refresh tokens; null on one that doesn't.
    val refreshToken: String? = null,
    // "DPoP" when the server bound [token] to the device key (RFC 9449); absent for a bearer token.
    val tokenType: String? = null,
    val email: String = "",
    val isActive: Boolean = false,
    val isPremium: Boolean = false,
    val premiumExpiresAt: String? = null,
    val errorCode: String? = null,
)

@Serializable
data class AuthResponse(
    val success: Boolean? = null,
    val status: Boolean = false,
    val message: String? = null,
    val data: AuthData? = null,
) {
    fun isSuccess(): Boolean = success ?: status
}

@Serializable
data class LogoutErrorData(
    val errorType: String? = null,
    val errorMessage: String? = null,
    val errorClass: String? = null,
    val endpoint: String? = null,
)

@Serializable
data class LogoutResponse(
    val message: String? = null,
    val data: LogoutErrorData? = null,
)
