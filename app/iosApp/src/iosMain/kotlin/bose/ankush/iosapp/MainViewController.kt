package bose.ankush.iosapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import bose.ankush.auth.presentation.AuthEffect
import bose.ankush.auth.presentation.AuthIntent
import bose.ankush.auth.presentation.AuthState
import bose.ankush.auth.presentation.AuthViewModel
import bose.ankush.auth.presentation.LoginScreen
import bose.ankush.commonui.components.NotificationToast
import bose.ankush.commonui.components.ToastType
import bose.ankush.commonui.components.rememberToastAnchorState
import bose.ankush.commonui.web.InAppWebView
import bose.ankush.home.HomeSessionCleaner
import bose.ankush.navigation.AppNavigation
import bose.ankush.payment.domain.store.PremiumStore
import bose.ankush.payment.presentation.PaymentViewModel
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

// Entry point called from Swift (iosApp/iosApp/iOSApp.swift).
fun MainViewController(): UIViewController =
    ComposeUIViewController {
        MaterialTheme {
            AppContent()
        }
    }

@Composable
private fun AppContent() {
    val authViewModel = koinInject<AuthViewModel>()
    val paymentViewModel = koinInject<PaymentViewModel>()
    val homeSessionCleaner = koinInject<HomeSessionCleaner>()
    val premiumStore = koinInject<PremiumStore>()

    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val isAuthInitialized by authViewModel.isAuthInitialized.collectAsState()
    val toastAnchorState = rememberToastAnchorState()

    var toastVisible by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf("") }
    var toastTitle by remember { mutableStateOf("") }
    var toastType by remember { mutableStateOf(ToastType.ERROR) }

    fun showToast(
        message: String,
        title: String = "Error",
        type: ToastType = ToastType.ERROR,
    ) {
        toastMessage = message
        toastTitle = title
        toastType = type
        toastVisible = true
    }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Error -> {
                showToast((authState as AuthState.Error).message, "Error", ToastType.ERROR)
                authViewModel.processIntent(AuthIntent.Reset)
            }

            is AuthState.Success -> {
                showToast("Authentication successful", "Success", ToastType.SUCCESS)
                authViewModel.processIntent(AuthIntent.Reset)
            }

            is AuthState.SessionExpired -> {
                showToast((authState as AuthState.SessionExpired).message, "Session Expired", ToastType.WARNING)
                authViewModel.processIntent(AuthIntent.Reset)
            }

            else -> Unit
        }
    }

    LaunchedEffect(Unit) {
        authViewModel.effect.collect { effect ->
            when (effect) {
                is AuthEffect.PremiumStatusChanged ->
                    premiumStore.savePremiumStatus(effect.isPremium, effect.expiryMillis)

                AuthEffect.LoggedOut -> {
                    homeSessionCleaner.clearOnLogout()
                    premiumStore.savePremiumStatus(isPremium = false, expiryMillis = null)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        authViewModel.processIntent(AuthIntent.RefreshToken)
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                ),
    ) {
        when {
            !isAuthInitialized -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }

            isLoggedIn -> {
                AppNavigation(
                    authViewModel = authViewModel,
                    paymentViewModel = paymentViewModel,
                    versionName = "1.0",
                    onShowToast = { message -> showToast(message) },
                    toastAnchorState = toastAnchorState,
                )
            }

            else -> {
                var currentWebUrl by remember { mutableStateOf<String?>(null) }
                if (currentWebUrl != null) {
                    InAppWebView(
                        url = currentWebUrl!!,
                        onClose = { currentWebUrl = null },
                    )
                } else {
                    LoginScreen(
                        onLoginClick = { email, password ->
                            authViewModel.processIntent(AuthIntent.Login(email, password))
                        },
                        onRegisterClick = { email, password ->
                            authViewModel.processIntent(AuthIntent.Register(email, password))
                        },
                        onWebUrlClick = { url -> currentWebUrl = url },
                        isLoading = authState is AuthState.Loading,
                    )
                }
            }
        }

        NotificationToast(
            modifier = Modifier.align(Alignment.BottomCenter),
            message = toastMessage,
            title = toastTitle,
            type = toastType,
            isVisible = toastVisible,
            onDismiss = { toastVisible = false },
            anchorState = toastAnchorState,
        )
    }
}
