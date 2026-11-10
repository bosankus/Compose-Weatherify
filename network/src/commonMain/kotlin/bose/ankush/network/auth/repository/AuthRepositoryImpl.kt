package bose.ankush.network.auth.repository

import bose.ankush.network.auth.api.AuthApiService
import bose.ankush.network.auth.dpop.authorizationScheme
import bose.ankush.network.auth.model.AuthResponse
import bose.ankush.network.auth.model.LoginRequest
import bose.ankush.network.auth.model.RefreshTokenRequest
import bose.ankush.network.auth.model.RegisterRequest
import bose.ankush.network.repository.PushTokenRegistration
import bose.ankush.tokenstorage.api.DpopKey
import bose.ankush.tokenstorage.api.TokenStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

class AuthRepositoryImpl(
    private val apiService: AuthApiService,
    private val tokenStorage: TokenStorage,
    private val dpopKey: DpopKey,
    // A provider, not the instance: registration needs the account API, which needs the
    // TokenManager, which needs this repository.
    private val pushTokens: () -> PushTokenRegistration? = { null },
) : AuthRepository {
    override suspend fun login(
        email: String,
        password: String,
    ): AuthResponse {
        startNewDeviceKey()
        val request = LoginRequest(email = email, password = password)
        val response = apiService.login(request)
        saveSession(response)
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
        startNewDeviceKey()
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
        saveSession(response)
        return response
    }

    override fun isLoggedIn(): Flow<Boolean> = tokenStorage.hasToken()

    override suspend fun getToken(): String? = tokenStorage.getToken()

    override suspend fun refreshToken(): AuthResponse? {
        val currentToken = tokenStorage.getToken() ?: return null

        val request =
            RefreshTokenRequest(token = currentToken, refreshToken = tokenStorage.getRefreshToken())
        val response = apiService.refreshToken(request)
        saveSession(response)
        return response
    }

    /**
     * Revokes the session on the server when it can, then always signs out here: the user's choice
     * wins even offline, and a session the server couldn't revoke lapses when its tokens expire.
     * A failure means only the server half failed; this device is signed out either way.
     */
    override suspend fun logout(): Result<Unit> {
        val serverResult =
            try {
                // While the session still authenticates: this device stops getting this account's pushes.
                pushTokens()?.unregister()
                val response =
                    apiService.logout(
                        tokenStorage.getToken(),
                        authorizationScheme(tokenStorage.getTokenType())
                    )
                if (response.data == null) {
                    Result.success(Unit)
                } else {
                    val message =
                        response.data.errorMessage?.takeIf { it.isNotBlank() }
                            ?: response.message
                            ?: "Logout failed"
                    Result.failure(Exception(message))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        tokenStorage.clearToken()
        return serverResult
    }

    /** Keeps the tokens from a successful sign-in or refresh; the refresh token only when issued. */
    private suspend fun saveSession(response: AuthResponse) {
        val data = response.data ?: return
        if (response.isSuccess() && data.token.isNotBlank()) {
            tokenStorage.saveTokens(
                accessToken = data.token,
                refreshToken = data.refreshToken?.takeIf { it.isNotBlank() },
                tokenType = data.tokenType?.takeIf { it.isNotBlank() },
            )
        }
    }

    /**
     * Each sign-in gets its own DPoP key, so sessions on this device share nothing. If the old key
     * can't be removed the new session keeps using it: still bound to this device.
     */
    private suspend fun startNewDeviceKey() {
        try {
            dpopKey.delete()
        } catch (e: CancellationException) {
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception,
        ) {
            // Keep the existing key; see above.
        }
    }
}
