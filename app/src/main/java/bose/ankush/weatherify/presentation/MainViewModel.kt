package bose.ankush.weatherify.presentation

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
        // Observe login state and set auth initialized
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
        // Load premium status and expiry from preferences
        viewModelScope.launch(dispatchers.io) {
            try {
                val prefs = preferenceManager.getLocationPreferenceFlow().first()
                val isPremiumStored = prefs[PreferenceManager.IS_PREMIUM] ?: false
                val expiry = prefs[PreferenceManager.PREMIUM_EXPIRY]
                val now = System.currentTimeMillis()
                val isActive = isPremiumStored && (expiry == null || expiry > now)
                withContext(dispatchers.main) {
                    _paymentUiState.update { state ->
                        state.copy(
                            isPremiumActivated = isActive,
                            expiryMillis = expiry,
                            stage = if (isActive) PaymentStage.Success else state.stage
                        )
                    }
                }
            } catch (_: Exception) {
                // ignore
            }
        }
    }

    /** Remove first permission dialog from queue. */
    fun dismissDialog() = permissionDialogQueue.removeAt(0)

    /** Handle permission result, fetch location if granted. */
    fun onPermissionResult(permission: String, isGranted: Boolean) {
        if (!isGranted && !permissionDialogQueue.contains(permission))
            permissionDialogQueue.add(permission)
        else fetchAndSaveLocationCoordinates()
    }

    /** Show/hide notification permission dialog. */
    fun updateNotificationPermission(launchState: Boolean) =
        _launchNotificationPermission.update { launchState }

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
        locationJob?.cancel()
        locationJob = viewModelScope.launch(dataFetchExceptionHandler + dispatchers.io) {
            try {
                locationClient.getCurrentLocation().fold(
                    onSuccess = { loc ->
                        preferenceManager.saveLocationPreferences(loc.latitude to loc.longitude)
                        performInitialDataLoading()
                    },
                    onFailure = { e ->
                        val error = if (e is Exception) errorResponseFromException(e)
                        else UiText.StringResource(resId = R.string.general_error_txt)
                        _uiState.update { UIState(error = error) }
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error fetching location coordinates")
                _uiState.update { it.copy(error = errorResponseFromException(e)) }
            }
        }
    }

    /** Load weather and air quality data for UI. */
    private fun performInitialDataLoading() {
        dataLoadingJob?.cancel()
        dataLoadingJob = viewModelScope.launch(dataFetchExceptionHandler + dispatchers.io) {
            try {
                val prefs = preferenceManager.getLocationPreferenceFlow().first()
                val lat = prefs[PreferenceManager.USER_LAT_LOCATION]
                val lon = prefs[PreferenceManager.USER_LON_LOCATION]
                if (lat != null && lon != null) {
                    val location = lat to lon
                    refreshWeatherReport(location)
                    getAirQuality(location.first, location.second)
                        .combine(getWeatherReport(location)) { air, weather ->
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
                        .onEach { state -> _uiState.value = state }
                        .launchIn(this)
                } else {
                    _uiState.update {
                        UIState(
                            isLoading = false,
                            error = UiText.StringResource(R.string.default_coordinates_txt)
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
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
        _authState.value = AuthState.Loading
        try {
            handleAuthResponse(authRepository.login(email, password))
        } catch (e: Exception) {
            _authState.value = AuthState.Error(UiText.DynamicText(e.message ?: "Login failed"))
        }
    }

    /** Register with email and password. */
    fun register(email: String, password: String) = viewModelScope.launch {
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
            handleAuthResponse(resp)
        } catch (e: Exception) {
            _authState.value =
                AuthState.Error(UiText.DynamicText(e.message ?: "Registration failed"))
        }
    }

    /** Logout user. */
    fun logout() = viewModelScope.launch {
        _authState.value = AuthState.LogoutLoading
        try {
            val result = authRepository.logout()
            _authState.value = if (result.isSuccess) AuthState.LoggedOut
            else AuthState.Error(
                UiText.DynamicText(
                    result.exceptionOrNull()?.message ?: "Logout failed"
                )
            )
        } catch (e: Exception) {
            _authState.value = AuthState.Error(UiText.DynamicText(e.message ?: "Logout failed"))
        }
    }

    /** Handle authentication response. */
    private fun handleAuthResponse(response: AuthResponse) {
        val token = response.data?.token
        _authState.value = if (response.isSuccess() && !token.isNullOrBlank())
            AuthState.Success
        else AuthState.Error(UiText.DynamicText(response.message ?: "Authentication failed"))
    }

    /** Reset authentication state. */
    fun resetAuthState() {
        _authState.value = AuthState.Initial
    }

    // --- Payment ---

    /** Start payment process. */
    fun startPayment(amountPaise: Long = 10_000L, currency: String = "INR") =
        viewModelScope.launch {
            _paymentUiState.value =
                _paymentUiState.value.copy(
                    loading = true,
                    message = "Creating order...",
                    stage = PaymentStage.CreatingOrder
                )
            try {
                val result = createOrder(
                    CreateOrderRequest(
                        amount = amountPaise,
                        currency = currency,
                        receipt = "receipt_${System.currentTimeMillis()}",
                        partialPayment = true,
                        firstPaymentMinAmount = 500L,
                        notes = mapOf("note1" to "This is a note", "note2" to "Another note")
                    )
                )
                result.fold(
                    onSuccess = { response ->
                        val data = response.extractData()
                        val key = BuildConfig.RAZORPAY_KEY
                        if (data == null) {
                            _paymentUiState.value = _paymentUiState.value.copy(
                                loading = false,
                                message = friendlyMessageFromServer(response.message),
                                stage = PaymentStage.Failure
                            )
                        } else if (key.isBlank()) {
                            _paymentUiState.value = _paymentUiState.value.copy(
                                loading = false,
                                message = "Payment is temporarily unavailable. Please try again later.",
                                stage = PaymentStage.Failure
                            )
                        } else if (data.orderId.isBlank() || data.amount <= 0L || data.currency.isBlank()) {
                            _paymentUiState.value = _paymentUiState.value.copy(
                                loading = false,
                                message = "We couldn't start the payment. Please try again.",
                                stage = PaymentStage.Failure
                            )
                        } else {
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
                    },
                    onFailure = { e ->
                        _paymentUiState.value = _paymentUiState.value.copy(
                            loading = false,
                            message = friendlyMessageFromThrowable(e),
                            stage = PaymentStage.Failure
                        )
                    }
                )
            } catch (e: Exception) {
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
                            val cal = Calendar.getInstance(TimeZone.getDefault())
                            cal.timeInMillis = System.currentTimeMillis()
                            cal.add(Calendar.MONTH, 1)
                            val expiry = cal.timeInMillis
                            withContext(dispatchers.io) {
                                preferenceManager.savePremiumStatus(true, expiry)
                            }
                            _paymentUiState.value = _paymentUiState.value.copy(
                                loading = false,
                                message = "Payment verified",
                                stage = PaymentStage.Success,
                                isPremiumActivated = true,
                                expiryMillis = expiry
                            )
                        } else {
                            _paymentUiState.value = _paymentUiState.value.copy(
                                loading = false,
                                message = friendlyMessageFromServer(resp.message),
                                stage = PaymentStage.Failure
                            )
                        }
                    },
                    onFailure = { e ->
                        _paymentUiState.value = _paymentUiState.value.copy(
                            loading = false,
                            message = friendlyMessageFromThrowable(e),
                            stage = PaymentStage.Failure
                        )
                    }
                )
            } catch (e: Exception) {
                _paymentUiState.value = _paymentUiState.value.copy(
                    loading = false,
                    message = friendlyMessageFromThrowable(e),
                    stage = PaymentStage.Failure
                )
            }
        }

    /** Handle payment failure. */
    fun onPaymentFailed(message: String) {
        _paymentUiState.value = _paymentUiState.value.copy(
            loading = false,
            message = friendlyMessageFromServer(message),
            stage = PaymentStage.Failure
        )
    }

    /** Cancel all jobs on ViewModel clear. */
    override fun onCleared() {
        super.onCleared()
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
