package bose.ankush.weatherify.presentation

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.network.auth.events.AuthEvent
import bose.ankush.network.auth.events.AuthEventBus.emit
import bose.ankush.network.auth.model.AuthResponse
import bose.ankush.network.auth.repository.AuthRepository
import bose.ankush.network.model.CreateOrderRequest
import bose.ankush.network.model.VerifyPaymentRequest
import bose.ankush.weatherify.BuildConfig
import bose.ankush.weatherify.R
import bose.ankush.weatherify.base.common.ENABLE_NOTIFICATION
import bose.ankush.weatherify.base.common.Extension
import bose.ankush.weatherify.base.common.UiText
import bose.ankush.weatherify.base.common.errorResponseFromException
import bose.ankush.weatherify.base.dispatcher.DispatcherProvider
import bose.ankush.weatherify.base.location.LocationClient
import bose.ankush.weatherify.domain.preference.PreferenceManager
import bose.ankush.weatherify.domain.remote_config.RemoteConfigService
import bose.ankush.weatherify.domain.use_case.get_air_quality.GetAirQuality
import bose.ankush.weatherify.domain.use_case.get_weather_reports.GetWeatherReport
import bose.ankush.weatherify.domain.use_case.payment.CreateOrder
import bose.ankush.weatherify.domain.use_case.payment.VerifyPayment
import bose.ankush.weatherify.domain.use_case.refresh_weather_reports.RefreshWeatherReport
import bose.ankush.weatherify.presentation.payment.PaymentEvent
import bose.ankush.weatherify.presentation.payment.PaymentStage
import bose.ankush.weatherify.presentation.payment.PaymentUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

/**
 * Main ViewModel for Weatherify.
 * Handles UI state, authentication, location, notifications, and payment.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val refreshWeatherReport: RefreshWeatherReport,
    private val getWeatherReport: GetWeatherReport,
    private val getAirQuality: GetAirQuality,
    private val locationClient: LocationClient,
    private val preferenceManager: PreferenceManager,
    private val dispatchers: DispatcherProvider,
    private val remoteConfigService: RemoteConfigService,
    private val authRepository: AuthRepository,
    private val createOrder: CreateOrder,
    private val verifyPayment: VerifyPayment
) : ViewModel() {

    private fun friendlyMessageFromThrowable(t: Throwable?): String {
        return when (t) {
            null -> "Something went wrong. Please try again."
            is CancellationException -> "Request was cancelled. Please try again."
            else -> "Something went wrong. Please try again."
        }
    }

    private fun friendlyMessageFromServer(message: String?): String {
        if (message.isNullOrBlank()) return "Something went wrong. Please try again."
        val lower = message.lowercase()
        return when {
            "timeout" in lower -> "The server took too long to respond. Please try again."
            "cancel" in lower -> "Payment was cancelled."
            "network" in lower || "unable to resolve host" in lower -> "Please check your internet connection and try again."
            else -> "Something went wrong. Please try again."
        }
    }

    // Permission dialog queue for UI
    var permissionDialogQueue = mutableStateListOf<String>()
        private set

    // UI state flows
    private val _uiState = MutableStateFlow(UIState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val _launchNotificationPermission = MutableStateFlow(false)
    val launchNotificationPermission = _launchNotificationPermission.asStateFlow()

    private val _showNotificationCardItem = MutableStateFlow(false)
    val showNotificationCardItem = _showNotificationCardItem.asStateFlow()

    // Auth state flows
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isAuthInitialized = MutableStateFlow(false)
    val isAuthInitialized: StateFlow<Boolean> = _isAuthInitialized.asStateFlow()

    // Payment state
    private val _paymentUiState = MutableStateFlow(PaymentUiState())
    val paymentUiState: StateFlow<PaymentUiState> = _paymentUiState.asStateFlow()

    private val _paymentEvents = Channel<PaymentEvent>(Channel.BUFFERED)
    val paymentEvents = _paymentEvents.receiveAsFlow()

    // Coroutine jobs
    private var notificationBannerJob: Job? = null
    private var locationJob: Job? = null
    private var dataLoadingJob: Job? = null

    private val tag = "${MainViewModel::class.simpleName} ->"

    // Exception handler for data fetch
    private val dataFetchExceptionHandler = CoroutineExceptionHandler { _, e ->
        if (e !is CancellationException) {
            val error = if (e is Exception) errorResponseFromException(e)
            else UiText.StringResource(resId = R.string.general_error_txt)
            _uiState.update { UIState(error = error) }
        }
    }

    init {
        Timber.tag(tag).d("MainViewModel initialized")

        // Observe login state and set auth initialized
        viewModelScope.launch {
            var initialized = false
            authRepository.isLoggedIn().collectLatest { loggedIn ->
                _isLoggedIn.value = loggedIn
                Timber.tag(tag).d("Auth state changed - isLoggedIn: $loggedIn")
                if (!initialized) {
                    _isAuthInitialized.value = true
                    initialized = true
                    Timber.tag(tag).d("Auth initialization completed")

                    // Silently refresh token in the background if user is logged in
                    if (loggedIn) {
                        silentTokenRefresh()
                    }
                }
            }
        }

        // Load premium status and expiry from preferences
        viewModelScope.launch(dispatchers.io) {
            try {
                Timber.tag(tag).d("Loading premium status from preferences")
                val prefs = preferenceManager.getLocationPreferenceFlow().first()
                val isPremiumStored = prefs[PreferenceManager.IS_PREMIUM] ?: false
                val expiry = prefs[PreferenceManager.PREMIUM_EXPIRY]
                val now = System.currentTimeMillis()
                val isActive = isPremiumStored && (expiry == null || expiry > now)

                Timber.tag(tag).d(
                    "Premium status loaded - stored: $isPremiumStored, expiry: $expiry, active: $isActive"
                )

                withContext(dispatchers.main) {
                    _paymentUiState.update { state ->
                        state.copy(
                            isPremiumActivated = isActive,
                            expiryMillis = expiry,
                            stage = if (isActive) PaymentStage.Success else state.stage
                        )
                    }
                    if (isActive) {
                        Timber.tag(tag).i("Premium status active on app start")
                    }
                }
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error loading premium status from preferences")
            }
        }
    }

    /** Remove first permission dialog from queue. */
    fun dismissDialog() {
        if (permissionDialogQueue.isNotEmpty()) {
            val dismissed = permissionDialogQueue.removeAt(0)
            Timber.tag(tag).d("Dismissed permission dialog: $dismissed")
        }
    }

    /** Handle permission result, fetch location if granted. */
    fun onPermissionResult(permission: String, isGranted: Boolean) {
        Timber.tag(tag).d("Permission result - permission: $permission, granted: $isGranted")
        if (!isGranted && !permissionDialogQueue.contains(permission)) {
            permissionDialogQueue.add(permission)
            Timber.tag(tag).w("Permission denied: $permission, added to queue")
        } else {
            Timber.tag(tag).d("Permission granted, fetching location")
            fetchAndSaveLocationCoordinates()
        }
    }

    /** Show/hide notification permission dialog. */
    fun updateNotificationPermission(launchState: Boolean) {
        Timber.tag(tag).d("Updating notification permission dialog - show: $launchState")
        _launchNotificationPermission.update { launchState }
    }

    /** Show/hide notification banner based on remote config. */
    fun updateShowNotificationBannerState(launchState: Boolean) {
        notificationBannerJob?.cancel()
        notificationBannerJob = viewModelScope.launch(dataFetchExceptionHandler + dispatchers.io) {
            try {
                val enabled = remoteConfigService.getBoolean(ENABLE_NOTIFICATION)
                _showNotificationCardItem.update { enabled && launchState }
                Timber.tag(tag)
                    .d("Notification feature is ${if (enabled) "enabled" else "disabled"}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error updating notification banner state")
                _uiState.update { it.copy(error = errorResponseFromException(e)) }
            }
        }
    }

    /** Fetch and save user location, then load initial data. */
    fun fetchAndSaveLocationCoordinates() {
        Timber.tag(tag).d("Starting location fetch")
        locationJob?.cancel()
        locationJob = viewModelScope.launch(dataFetchExceptionHandler + dispatchers.io) {
            try {
                locationClient.getCurrentLocation().fold(
                    onSuccess = { loc ->
                        Timber.tag(tag).i(
                            "Location fetched successfully - lat: ${loc.latitude}, lon: ${loc.longitude}"
                        )
                        preferenceManager.saveLocationPreferences(loc.latitude to loc.longitude)
                        Timber.tag(tag).d("Location preferences saved")
                        performInitialDataLoading()
                    },
                    onFailure = { e ->
                        Timber.tag(tag).e(e, "Location fetch failed")
                        val error = if (e is Exception) errorResponseFromException(e)
                        else UiText.StringResource(resId = R.string.general_error_txt)
                        _uiState.update { it.copy(error = error) }
                    }
                )
            } catch (e: CancellationException) {
                Timber.tag(tag).d("Location fetch cancelled")
                _uiState.update { it.copy(error = errorResponseFromException(e)) }
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error fetching location coordinates")
                _uiState.update { it.copy(error = errorResponseFromException(e)) }
            }
        }
    }

    /** Load weather and air quality data for UI. */
    private fun performInitialDataLoading() {
        Timber.tag(tag).d("Starting initial data loading")
        dataLoadingJob?.cancel()
        dataLoadingJob = viewModelScope.launch(dataFetchExceptionHandler + dispatchers.io) {
            try {
                val prefs = preferenceManager.getLocationPreferenceFlow().first()
                val lat = prefs[PreferenceManager.USER_LAT_LOCATION]
                val lon = prefs[PreferenceManager.USER_LON_LOCATION]

                if (lat != null && lon != null) {
                    val location = lat to lon
                    Timber.tag(tag).d("Loading data for location - lat: $lat, lon: $lon")

                    refreshWeatherReport(location)
                    Timber.tag(tag).v("Refreshed weather report cache")

                    getAirQuality(location.first, location.second)
                        .combine(getWeatherReport(location)) { air, weather ->
                            Timber.tag(tag).d("Data loaded successfully")
                            UIState(
                                isLoading = false,
                                userLocation = location,
                                weatherData = weather,
                                airQualityData = air,
                                error = null
                            )
                        }
                        .flowOn(dispatchers.io)
                        .catch { e ->
                            if (e is CancellationException) throw e
                            Timber.tag(tag).e(e, "Error loading weather data")
                            val error = if (e is Exception) errorResponseFromException(e)
                            else UiText.StringResource(resId = R.string.general_error_txt)
                            _uiState.update { it.copy(isLoading = false, error = error) }
                        }
                        .collectLatest { state -> _uiState.value = state }
                } else {
                    Timber.tag(tag).w("Location coordinates not found in preferences")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = UiText.StringResource(R.string.default_coordinates_txt)
                        )
                    }
                }
            } catch (e: CancellationException) {
                Timber.tag(tag).d("Initial data loading cancelled")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = errorResponseFromException(e)
                    )
                }
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error in initial data loading")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = errorResponseFromException(e)
                    )
                }
            }
        }
    }

    /** Login with email and password. */
    fun login(email: String, password: String) = viewModelScope.launch {
        Timber.tag(tag).d("Login attempt for email: $email")
        _authState.value = AuthState.Loading
        try {
            handleAuthResponse(authRepository.login(email, password))
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Login failed for email: $email")
            _authState.value = AuthState.Error(UiText.DynamicText(e.message ?: "Login failed"))
        }
    }

    /** Register with email and password. */
    fun register(email: String, password: String) = viewModelScope.launch {
        Timber.tag(tag).d("Registration attempt for email: $email")
        _authState.value = AuthState.Loading
        try {
            val resp = authRepository.register(
                email = email,
                password = password,
                timestampOfRegistration = Extension.getCurrentUtcTimestamp(),
                deviceModel = Extension.getDeviceModel(),
                operatingSystem = Extension.getOperatingSystem(),
                osVersion = Extension.getOsVersion(),
                appVersion = Extension.getAppVersion(),
                ipAddress = Extension.getIpAddress(),
                registrationSource = Extension.getRegistrationSource(),
                firebaseToken = Extension.getFirebaseToken()
            )
            Timber.tag(tag).d("Registration response received for email: $email")
            handleAuthResponse(resp)
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Registration failed for email: $email")
            _authState.value =
                AuthState.Error(UiText.DynamicText(e.message ?: "Registration failed"))
        }
    }

    /** Logout user. */
    fun logout() = viewModelScope.launch {
        Timber.tag(tag).d("Logout initiated")
        _authState.value = AuthState.LogoutLoading
        try {
            val result = authRepository.logout()
            if (result.isSuccess) {
                Timber.tag(tag).i("Logout successful")
                _authState.value = AuthState.LoggedOut
            } else {
                Timber.tag(tag).e(result.exceptionOrNull(), "Logout failed")
                _authState.value = AuthState.Error(
                    UiText.DynamicText(
                        result.exceptionOrNull()?.message ?: "Logout failed"
                    )
                )
            }
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Logout exception")
            _authState.value = AuthState.Error(UiText.DynamicText(e.message ?: "Logout failed"))
        }
    }

    /** Silently refresh token in the background (on app startup). */
    private fun silentTokenRefresh() = viewModelScope.launch(dispatchers.io) {
        try {
            Timber.tag(tag).d("Starting silent token refresh")
            val response = authRepository.refreshToken()

            when {
                response == null -> {
                    Timber.tag(tag).w("Token refresh failed - null response")
                    emit(AuthEvent.Unauthorized("Your session has expired. Please log in again."))
                }

                response.isSuccess() -> {
                    Timber.tag(tag).i("Token refreshed successfully")
                }

                response.data?.errorCode == "TOKEN_NOT_EXPIRED" -> {
                    // Token is still valid — no action needed
                    Timber.tag(tag).d("Token is still valid, no refresh needed")
                }

                else -> {
                    Timber.tag(tag).w("Token refresh failed - response: ${response.message}")
                    emit(AuthEvent.Unauthorized("Your session has expired. Please log in again."))
                }
            }
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Silent token refresh error")
            emit(AuthEvent.Unauthorized("Your session has expired. Please log in again."))
        }
    }

    /** Handle authentication response. */
    private fun handleAuthResponse(response: AuthResponse) {
        val token = response.data?.token
        if (response.isSuccess() && !token.isNullOrBlank()) {
            Timber.tag(tag).i("Authentication successful")
            val data = response.data
            if (data != null && data.isPremium) {
                viewModelScope.launch {
                    Timber.tag(tag).i("User is premium, saving premium status")
                    val expiryMillis = data.premiumExpiresAt
                        ?.let { parseIsoToMillis(it) }
                        ?: (System.currentTimeMillis() + (365 * 24 * 60 * 60 * 1000L))
                    preferenceManager.savePremiumStatus(
                        isPremium = true,
                        expiryMillis = expiryMillis
                    )
                    _paymentUiState.value = _paymentUiState.value.copy(isPremiumActivated = true)
                }
            }
            _authState.value = AuthState.Success
        } else {
            Timber.tag(tag)
                .w("Authentication failed - success: ${response.isSuccess()}, has token: ${!token.isNullOrBlank()}")
            _authState.value =
                AuthState.Error(UiText.DynamicText(response.message ?: "Authentication failed"))
        }
    }

    private fun parseIsoToMillis(isoDate: String): Long? = try {
        java.time.Instant.parse(isoDate).toEpochMilli()
    } catch (_: Exception) {
        null
    }

    /** Reset authentication state. */
    fun resetAuthState() {
        Timber.tag(tag).d("Auth state reset to Initial")
        _authState.value = AuthState.Initial
    }

    // --- Payment ---

    /** Start payment process. */
    fun startPayment(amountPaise: Long = 10_000L, currency: String = "INR") =
        viewModelScope.launch {
            Timber.tag(tag).d("Starting payment process - amount: ${amountPaise / 100.0} $currency")

            _paymentUiState.value =
                _paymentUiState.value.copy(
                    loading = true,
                    message = "Creating order...",
                    stage = PaymentStage.CreatingOrder
                )

            try {
                val receipt = "receipt_${System.currentTimeMillis()}"
                Timber.tag(tag).v("Creating order with receipt: $receipt")

                val result = createOrder(
                    CreateOrderRequest(
                        amount = amountPaise,
                        currency = currency,
                        receipt = receipt,
                        partialPayment = true,
                        firstPaymentMinAmount = 500L,
                        notes = mapOf("note1" to "This is a note", "note2" to "Another note")
                    )
                )

                result.fold(
                    onSuccess = { response ->
                        val data = response.extractData()
                        val key = BuildConfig.RAZORPAY_KEY

                        when {
                            data == null -> {
                                Timber.tag(tag).e("Order creation failed - no data in response")
                                _paymentUiState.value = _paymentUiState.value.copy(
                                    loading = false,
                                    message = friendlyMessageFromServer(response.message),
                                    stage = PaymentStage.Failure
                                )
                            }

                            key.isBlank() -> {
                                Timber.tag(tag).e("Razorpay key not configured")
                                _paymentUiState.value = _paymentUiState.value.copy(
                                    loading = false,
                                    message = "Payment is temporarily unavailable. Please try again later.",
                                    stage = PaymentStage.Failure
                                )
                            }

                            data.orderId.isBlank() || data.amount <= 0L || data.currency.isBlank() -> {
                                Timber.tag(tag)
                                    .e("Invalid order data - orderId: ${data.orderId.isNotBlank()}, amount: ${data.amount > 0}, currency: ${data.currency.isNotBlank()}")
                                _paymentUiState.value = _paymentUiState.value.copy(
                                    loading = false,
                                    message = "We couldn't start the payment. Please try again.",
                                    stage = PaymentStage.Failure
                                )
                            }

                            else -> {
                                Timber.tag(tag)
                                    .i("Order created successfully - orderId: ${data.orderId}")
                                _paymentEvents.trySend(
                                    PaymentEvent.LaunchCheckout(
                                        keyId = key,
                                        orderId = data.orderId,
                                        amount = data.amount,
                                        currency = data.currency,
                                        name = "Weatherify Subscription",
                                        description = "Premium Plan"
                                    )
                                )
                                _paymentUiState.value = _paymentUiState.value.copy(
                                    loading = false,
                                    message = response.message ?: "Order created",
                                    stage = PaymentStage.AwaitingPayment
                                )
                            }
                        }
                    },
                    onFailure = { e ->
                        Timber.tag(tag).e(e, "Order creation failed")
                        _paymentUiState.value = _paymentUiState.value.copy(
                            loading = false,
                            message = friendlyMessageFromThrowable(e),
                            stage = PaymentStage.Failure
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Exception during payment start")
                _paymentUiState.value = _paymentUiState.value.copy(
                    loading = false,
                    message = friendlyMessageFromThrowable(e),
                    stage = PaymentStage.Failure
                )
            }
        }

    /** Verify payment. */
    fun verifyPayment(orderId: String, paymentId: String, signature: String) =
        viewModelScope.launch {
            Timber.tag(tag)
                .d("Starting payment verification - orderId: $orderId, paymentId: $paymentId")

            _paymentUiState.value =
                _paymentUiState.value.copy(
                    loading = true,
                    message = "Verifying payment...",
                    stage = PaymentStage.Verifying
                )

            try {
                val result = verifyPayment(
                    VerifyPaymentRequest(
                        razorpayOrderId = orderId,
                        razorpayPaymentId = paymentId,
                        razorpaySignature = signature
                    )
                )

                result.fold(
                    onSuccess = { resp ->
                        if (resp.success) {
                            Timber.tag(tag).i("Payment verified successfully")
                            val cal = Calendar.getInstance(TimeZone.getDefault())
                            cal.timeInMillis = System.currentTimeMillis()
                            cal.add(Calendar.MONTH, 1)
                            val expiry = cal.timeInMillis

                            withContext(dispatchers.io) {
                                try {
                                    preferenceManager.savePremiumStatus(true, expiry)
                                    Timber.tag(tag)
                                        .d("Premium status saved to preferences - expiry: $expiry")
                                } catch (e: Exception) {
                                    Timber.tag(tag).e(e, "Error saving premium status")
                                }
                            }

                            _paymentUiState.value = _paymentUiState.value.copy(
                                loading = false,
                                message = "Payment verified",
                                stage = PaymentStage.Success,
                                isPremiumActivated = true,
                                expiryMillis = expiry
                            )
                        } else {
                            Timber.tag(tag).e("Payment verification failed - success: false")
                            _paymentUiState.value = _paymentUiState.value.copy(
                                loading = false,
                                message = friendlyMessageFromServer(resp.message),
                                stage = PaymentStage.Failure
                            )
                        }
                    },
                    onFailure = { e ->
                        Timber.tag(tag).e(e, "Payment verification failed")
                        _paymentUiState.value = _paymentUiState.value.copy(
                            loading = false,
                            message = friendlyMessageFromThrowable(e),
                            stage = PaymentStage.Failure
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Exception during payment verification")
                _paymentUiState.value = _paymentUiState.value.copy(
                    loading = false,
                    message = friendlyMessageFromThrowable(e),
                    stage = PaymentStage.Failure
                )
            }
        }

    /** Handle payment failure. */
    fun onPaymentFailed(message: String) {
        Timber.tag(tag).e("Payment failed - message: $message")
        _paymentUiState.value = _paymentUiState.value.copy(
            loading = false,
            message = friendlyMessageFromServer(message),
            stage = PaymentStage.Failure
        )
    }

    /** Cancel all jobs on ViewModel clear. */
    override fun onCleared() {
        super.onCleared()
        Timber.tag(tag).d("MainViewModel cleared - cancelling all jobs")
        notificationBannerJob?.cancel()
        locationJob?.cancel()
        dataLoadingJob?.cancel()
    }
}

/** Authentication state. */
sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    object LogoutLoading : AuthState()
    object Success : AuthState()
    object LoggedOut : AuthState()
    data class Error(val message: UiText) : AuthState()
}
