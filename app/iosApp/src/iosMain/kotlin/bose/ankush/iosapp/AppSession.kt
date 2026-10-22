package bose.ankush.iosapp

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import bose.ankush.auth.ios.LoginScreenController
import bose.ankush.auth.presentation.AuthEffect
import bose.ankush.auth.presentation.AuthIntent
import bose.ankush.auth.presentation.AuthState
import bose.ankush.auth.presentation.AuthViewModel
import bose.ankush.home.HomeSessionCleaner
import bose.ankush.home.PlaceEventSyncTrigger
import bose.ankush.iosapp.payment.RazorpayCheckoutBridge
import bose.ankush.payment.presentation.PaymentEffect
import bose.ankush.payment.presentation.PaymentIntent
import bose.ankush.payment.presentation.PaymentUiState
import bose.ankush.payment.presentation.PaymentViewModel
import bose.ankush.settings.ios.SettingsScreenController
import bose.ankush.settings.ios.SettingsSession
import bose.ankush.storage.api.PremiumStorage
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import platform.UIKit.UIViewController

/**
 * App-wide sign-in state for the SwiftUI shell. Owns the one [AuthViewModel] and
 * [PaymentViewModel] every screen shares (sign-in, Settings), and carries out their app-level effects:
 * premium status, logout and account-deletion cleanup, and the Razorpay checkout. Swift creates one at launch.
 */
class AppSession :
    KoinComponent,
    SettingsSession {
    private val scope = MainScope()
    private val store = ViewModelStore()
    private val provider =
        ViewModelProvider.create(
            store,
            viewModelFactory {
                initializer { get<AuthViewModel>() }
                initializer { get<PaymentViewModel>() }
            },
        )
    private val auth: AuthViewModel = provider[AuthViewModel::class]
    private val paymentViewModel: PaymentViewModel = provider[PaymentViewModel::class]

    val isLoggedIn: StateFlow<Boolean> = auth.isLoggedIn
    val isAuthInitialized: StateFlow<Boolean> = auth.isAuthInitialized

    override val payment: StateFlow<PaymentUiState> = paymentViewModel.uiState
    override val isLoggingOut: StateFlow<Boolean> =
        auth.authState
            .map { it is AuthState.LogoutLoading }
            .stateIn(scope, SharingStarted.Eagerly, false)

    override fun logout() = auth.processIntent(AuthIntent.Logout)

    override fun refreshPremium() = auth.processIntent(AuthIntent.RefreshToken)

    override fun startPayment(amountPaise: Long) =
        paymentViewModel.processIntent(PaymentIntent.StartPayment(amountPaise))

    /** A controller for the SwiftUI Settings page, bound to this session. */
    fun settingsController(): SettingsScreenController = SettingsScreenController(this)

    /** A controller for the SwiftUI sign-in screen, on this session's auth state. */
    fun loginController(): LoginScreenController = LoginScreenController(auth)

    init {
        val premiumStorage = get<PremiumStorage>()
        val homeSessionCleaner = get<HomeSessionCleaner>()
        scope.launch {
            auth.effect.collect { effect ->
                when (effect) {
                    is AuthEffect.PremiumStatusChanged ->
                        premiumStorage.savePremiumStatus(effect.isPremium, effect.expiryMillis)

                    // No push token to rotate on iOS: registration never sends one from here.
                    AuthEffect.LoggedOut, AuthEffect.AccountDeleted -> {
                        homeSessionCleaner.clearOnLogout()
                        premiumStorage.savePremiumStatus(isPremium = false, expiryMillis = null)
                    }
                }
            }
        }
        scope.launch {
            paymentViewModel.effect.collect { effect ->
                when (effect) {
                    is PaymentEffect.LaunchCheckout -> launchCheckout(effect)
                }
            }
        }
        scope.launch {
            // Settings logged out; reset so the sign-in screen starts clean (the Compose route did this).
            auth.authState
                .filter { it is AuthState.LoggedOut }
                .collect { auth.processIntent(AuthIntent.Reset) }
        }
        auth.processIntent(AuthIntent.RefreshToken)
        startPlaceEventSync()
    }

    /**
     * Place-event sync on iOS: once at launch and whenever the app returns to the foreground.
     * No background tasks. The observer lives as long as this app-wide session.
     */
    private fun startPlaceEventSync() {
        val placeEventSync = get<PlaceEventSyncTrigger>()
        placeEventSync.requestSync()
        NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationWillEnterForegroundNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { _ -> placeEventSync.requestSync() }
    }

    private fun launchCheckout(effect: PaymentEffect.LaunchCheckout) {
        val controller = topViewController() ?: return
        RazorpayCheckoutBridge().launch(
            controller = controller,
            params = effect.params,
            onSuccess = { orderId, paymentId, signature ->
                paymentViewModel.processIntent(
                    PaymentIntent.VerifyPayment(
                        orderId,
                        paymentId,
                        signature,
                    ),
                )
            },
            onError = { message ->
                paymentViewModel.processIntent(
                    PaymentIntent.PaymentFailed(
                        message,
                    ),
                )
            },
        )
    }
}

/** The controller on top of the key window, so checkout presents over whatever is showing. */
@Suppress("DEPRECATION")
private fun topViewController(): UIViewController? {
    var top = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (top?.presentedViewController != null) top = top.presentedViewController
    return top
}
