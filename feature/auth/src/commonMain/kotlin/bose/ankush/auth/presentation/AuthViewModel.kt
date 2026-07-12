package bose.ankush.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.analytics.AnalyticsEvent
import bose.ankush.analytics.AnalyticsTracker
import bose.ankush.auth.domain.DeviceInfoProvider
import bose.ankush.network.auth.events.AuthEvent
import bose.ankush.network.auth.events.AuthEventBus
import bose.ankush.network.auth.events.AuthEventBus.emit
import bose.ankush.network.auth.model.AuthResponse
import bose.ankush.network.auth.repository.AuthRepository
import bose.ankush.network.auth.token.TokenManager
import bose.ankush.network.auth.utils.isPremiumActive
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Instant

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isAuthInitialized = MutableStateFlow(false)
    val isAuthInitialized: StateFlow<Boolean> = _isAuthInitialized.asStateFlow()

    private val _effect = Channel<AuthEffect>(Channel.BUFFERED)
    val effect: Flow<AuthEffect> = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            var initialized = false
            authRepository.isLoggedIn().collectLatest { loggedIn ->
                _isLoggedIn.value = loggedIn
                if (!initialized) {
                    _isAuthInitialized.value = true
                    initialized = true
                }
            }
        }

        viewModelScope.launch {
            AuthEventBus.events.collect { event ->
                if (event is AuthEvent.Unauthorized) {
                    _authState.update {
                        AuthState.SessionExpired(
                            event.message.ifBlank {
                                "You need to log in again to continue using the app for security purposes."
                            },
                        )
                    }
                }
            }
        }
    }

    fun processIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.Login -> login(intent.email, intent.password)
            is AuthIntent.Register -> register(intent.email, intent.password)
            is AuthIntent.Logout -> logout()
            is AuthIntent.Reset -> _authState.value = AuthState.Initial
            is AuthIntent.RefreshToken -> refreshTokenOnForeground()
        }
    }

    private fun login(
        email: String,
        password: String,
    ) = launchAuth("Login", AnalyticsEvent.Login()) { authRepository.login(email, password) }

    private fun register(
        email: String,
        password: String,
    ) = launchAuth("Registration", AnalyticsEvent.SignUp()) {
        authRepository.register(
            email = email,
            password = password,
            timestampOfRegistration = deviceInfoProvider.getCurrentUtcTimestamp(),
            deviceModel = deviceInfoProvider.getDeviceModel(),
            operatingSystem = deviceInfoProvider.getOperatingSystem(),
            osVersion = deviceInfoProvider.getOsVersion(),
            appVersion = deviceInfoProvider.getAppVersion(),
            registrationSource = deviceInfoProvider.getRegistrationSource(),
            firebaseToken = deviceInfoProvider.getFirebaseToken(),
        )
    }

    private fun launchAuth(
        actionName: String,
        successEvent: AnalyticsEvent,
        block: suspend () -> AuthResponse,
    ) = viewModelScope.launch {
        _authState.value = AuthState.Loading
        try {
            handleAuthResponse(block(), successEvent)
        } catch (_: CancellationException) {
            _authState.value = AuthState.Error("$actionName was cancelled")
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "$actionName failed")
        }
    }

    private fun logout() =
        viewModelScope.launch {
            _authState.value = AuthState.LogoutLoading
            authRepository.logout().fold(
                onSuccess = {
                    analyticsTracker.track(AnalyticsEvent.Logout)
                    _effect.trySend(AuthEffect.LoggedOut)
                    _authState.value = AuthState.LoggedOut
                },
                onFailure = { e ->
                    _authState.value = AuthState.Error(e.message ?: "Logout failed")
                },
            )
        }

    private fun refreshTokenOnForeground() =
        viewModelScope.launch {
            try {
                val response = authRepository.refreshToken() ?: return@launch
                if (response.isSuccess()) {
                    val expiryMillis = response.data?.premiumExpiresAt?.let { parseIsoToMillis(it) }
                    val active = isPremiumActive(response.data?.premiumExpiresAt)
                    _effect.trySend(AuthEffect.PremiumStatusChanged(active, expiryMillis))
                } else {
                    tokenManager.forceLogout()
                    emit(AuthEvent.Unauthorized("Your session has expired. Please log in again."))
                }
            } catch (_: CancellationException) {
                // No implementation required
            } catch (_: Exception) {
                // No implementation required
            }
        }

    private fun handleAuthResponse(
        response: AuthResponse,
        successEvent: AnalyticsEvent,
    ) {
        val data =
            response.data
                ?.takeIf { response.isSuccess() && it.token.isNotBlank() }
                ?: run {
                    _authState.value = AuthState.Error(response.message ?: "Authentication failed")
                    return
                }

        val premiumActive = isPremiumActive(data.premiumExpiresAt)
        val expiryMillis = data.premiumExpiresAt?.let { parseIsoToMillis(it) }

        analyticsTracker.track(successEvent)
        _effect.trySend(AuthEffect.PremiumStatusChanged(premiumActive, expiryMillis))
        _authState.value = AuthState.Success
    }

    private fun parseIsoToMillis(isoDate: String): Long? =
        try {
            Instant.parse(isoDate).toEpochMilliseconds()
        } catch (_: Exception) {
            null
        }
}
