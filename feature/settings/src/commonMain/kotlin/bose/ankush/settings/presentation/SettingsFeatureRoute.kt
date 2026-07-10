package bose.ankush.settings.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bose.ankush.commonui.components.NotificationToast
import bose.ankush.commonui.components.ToastAnchorState
import bose.ankush.commonui.components.ToastType
import bose.ankush.commonui.web.InAppWebView
import bose.ankush.network.model.DurationType
import bose.ankush.network.model.Feature
import bose.ankush.network.model.PricingTier
import bose.ankush.network.model.Service
import bose.ankush.network.model.ServiceStatus
import bose.ankush.payment.presentation.PaymentStage
import bose.ankush.payment.presentation.PaymentUiState
import bose.ankush.settings.generated.resources.Res
import bose.ankush.settings.generated.resources.back_button_content
import bose.ankush.settings.generated.resources.cancel_btn_txt
import bose.ankush.settings.generated.resources.confirm_btn_txt
import bose.ankush.settings.generated.resources.logout_btn_txt
import bose.ankush.settings.generated.resources.logout_confirmation_txt
import bose.ankush.settings.generated.resources.premium_activated_msg_txt
import bose.ankush.settings.generated.resources.premium_activated_title_txt
import bose.ankush.settings.generated.resources.profile_title
import bose.ankush.settings.presentation.component.LegalSection
import bose.ankush.settings.presentation.component.PremiumCard
import bose.ankush.settings.presentation.component.SettingsSection
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.milliseconds

/**
 * Public entry point for the Settings feature. Bridges to the host app via plain
 * callbacks/params — [SettingsViewModel] itself stays internal, resolved through Koin and scoped
 * to wherever this route is composed (mirrors `feature:home`'s `HomeFeatureRoute`).
 */
@Composable
fun SettingsFeatureRoute(
    paymentUiState: PaymentUiState,
    isLoggingOut: Boolean,
    isLoggedOut: Boolean,
    versionName: String,
    shouldShowNotificationItem: Boolean,
    languageList: Array<String>,
    onLogout: () -> Unit,
    onLoggedOutHandled: () -> Unit,
    onStartPayment: (amountPaise: Long) -> Unit,
    onBackNavAction: () -> Unit,
    onLanguageNavAction: (Array<String>) -> Unit,
    onNotificationNavAction: () -> Unit,
    onBottomBarVisibilityChange: (Boolean) -> Unit = {},
    toastAnchorState: ToastAnchorState? = null,
    bottomBar: @Composable () -> Unit = {},
) {
    val viewModel = koinViewModel<SettingsViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val previousPaymentStage = remember { mutableStateOf(paymentUiState.stage) }

    LaunchedEffect(paymentUiState.stage) {
        when (paymentUiState.stage) {
            PaymentStage.CreatingOrder, PaymentStage.AwaitingPayment, PaymentStage.Verifying ->
                viewModel.processIntent(SettingsIntent.ClosePremiumSheet)

            PaymentStage.Success if previousPaymentStage.value != PaymentStage.Success ->
                viewModel.processIntent(SettingsIntent.PremiumActivated)

            PaymentStage.Failure ->
                viewModel.processIntent(SettingsIntent.ClosePremiumSheet)

            else -> Unit
        }
        previousPaymentStage.value = paymentUiState.stage
    }

    LaunchedEffect(isLoggedOut) {
        if (isLoggedOut) {
            viewModel.processIntent(SettingsIntent.CloseLogoutDialog)
            onLoggedOutHandled()
        }
    }

    SettingsScreenContent(
        paymentUiState = paymentUiState,
        isLoggingOut = isLoggingOut,
        versionName = versionName,
        shouldShowNotificationItem = shouldShowNotificationItem,
        state = state,
        onLogout = onLogout,
        onBackNavAction = onBackNavAction,
        onLanguageNavAction = { onLanguageNavAction(languageList) },
        onNotificationNavAction = onNotificationNavAction,
        onStartPayment = onStartPayment,
        onBottomBarVisibilityChange = onBottomBarVisibilityChange,
        onOpenPremiumSheet = { viewModel.processIntent(SettingsIntent.OpenPremiumSheet) },
        onClosePremiumSheet = { viewModel.processIntent(SettingsIntent.ClosePremiumSheet) },
        onOpenLogoutDialog = { viewModel.processIntent(SettingsIntent.OpenLogoutDialog) },
        onCloseLogoutDialog = { viewModel.processIntent(SettingsIntent.CloseLogoutDialog) },
        onOpenWebUrl = { url -> viewModel.processIntent(SettingsIntent.OpenWebUrl(url)) },
        onCloseWebView = { viewModel.processIntent(SettingsIntent.CloseWebView) },
        onDismissPremiumActivationToast = {
            viewModel.processIntent(SettingsIntent.DismissPremiumActivationToast)
        },
        onLoadServices = { viewModel.processIntent(SettingsIntent.LoadServices) },
        onSelectService = { viewModel.processIntent(SettingsIntent.SelectService(it)) },
        onSelectTier = { viewModel.processIntent(SettingsIntent.SelectTier(it)) },
        toastAnchorState = toastAnchorState,
        bottomBar = bottomBar,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreenContent(
    paymentUiState: PaymentUiState,
    isLoggingOut: Boolean,
    versionName: String,
    shouldShowNotificationItem: Boolean,
    state: SettingsState,
    onLogout: () -> Unit,
    onBackNavAction: () -> Unit,
    onLanguageNavAction: () -> Unit,
    onNotificationNavAction: () -> Unit,
    onStartPayment: (amountPaise: Long) -> Unit,
    onBottomBarVisibilityChange: (Boolean) -> Unit,
    onOpenPremiumSheet: () -> Unit,
    onClosePremiumSheet: () -> Unit,
    onOpenLogoutDialog: () -> Unit,
    onCloseLogoutDialog: () -> Unit,
    onOpenWebUrl: (String) -> Unit,
    onCloseWebView: () -> Unit,
    onDismissPremiumActivationToast: () -> Unit,
    onLoadServices: () -> Unit,
    onSelectService: (Service) -> Unit,
    onSelectTier: (PricingTier) -> Unit,
    toastAnchorState: ToastAnchorState? = null,
    bottomBar: @Composable () -> Unit = {},
) {
    val hasPlayedIntroAnimation = remember { SettingsAnimationState.hasPlayedIntroAnimation }
    val settingsSectionState = remember { MutableTransitionState(hasPlayedIntroAnimation) }
    val legalSectionState = remember { MutableTransitionState(hasPlayedIntroAnimation) }
    val logoutButtonState = remember { MutableTransitionState(hasPlayedIntroAnimation) }

    LaunchedEffect(state.showPremiumBottomSheet) {
        onBottomBarVisibilityChange(!state.showPremiumBottomSheet)
    }

    LaunchedEffect(Unit) {
        if (!hasPlayedIntroAnimation) {
            val sectionRevealState =
                listOf(settingsSectionState, legalSectionState, logoutButtonState)
            sectionRevealState.forEachIndexed { index, transitionState ->
                delay(if (index == 0) 100.milliseconds else 150.milliseconds)
                transitionState.targetState = true
            }
            SettingsAnimationState.hasPlayedIntroAnimation = true
        }
    }

    val currentWebUrl = state.currentWebUrl
    if (currentWebUrl != null) {
        InAppWebView(
            url = currentWebUrl,
            onClose = onCloseWebView,
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                stringResource(Res.string.profile_title),
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBackNavAction) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = stringResource(Res.string.back_button_content),
                                )
                            }
                        },
                        colors =
                            TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                    )
                },
                content = { innerPadding ->
                    LazyColumn(
                        modifier =
                            Modifier
                                .padding(innerPadding)
                                .padding(horizontal = 16.dp),
                    ) {
                        item { Spacer(modifier = Modifier.height(24.dp)) }

                        item {
                            PremiumCard(
                                paymentUiState = paymentUiState,
                                onClick = onOpenPremiumSheet,
                            )
                        }

                        item { Spacer(modifier = Modifier.height(24.dp)) }

                        item {
                            AnimatedVisibility(
                                visibleState = settingsSectionState,
                                enter = enterFromBottom,
                                exit = fadeOut(),
                            ) {
                                SettingsSection(
                                    shouldShowNotificationItem = shouldShowNotificationItem,
                                    onNotificationNavAction = onNotificationNavAction,
                                    onLanguageNavAction = onLanguageNavAction,
                                )
                            }
                        }

                        item { Spacer(modifier = Modifier.height(24.dp)) }

                        item {
                            AnimatedVisibility(
                                visibleState = legalSectionState,
                                enter = enterFromBottom,
                                exit = fadeOut(),
                            ) {
                                LegalSection(
                                    versionName = versionName,
                                    onUrlClick = onOpenWebUrl,
                                )
                            }
                        }

                        item { Spacer(modifier = Modifier.height(24.dp)) }

                        item {
                            AnimatedVisibility(
                                visibleState = logoutButtonState,
                                enter = enterFromBottom,
                                exit = fadeOut(),
                            ) {
                                TextButton(
                                    onClick = onOpenLogoutDialog,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = stringResource(Res.string.logout_btn_txt),
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }

                    if (state.showLogoutDialog) {
                        AlertDialog(
                            onDismissRequest = {
                                if (!isLoggingOut) onCloseLogoutDialog()
                            },
                            title = { Text(text = stringResource(Res.string.logout_btn_txt)) },
                            text = { Text(text = stringResource(Res.string.logout_confirmation_txt)) },
                            confirmButton = {
                                TextButton(onClick = onLogout, enabled = !isLoggingOut) {
                                    Text(stringResource(Res.string.confirm_btn_txt))
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = onCloseLogoutDialog,
                                    enabled = !isLoggingOut,
                                ) {
                                    Text(stringResource(Res.string.cancel_btn_txt))
                                }
                            },
                        )
                    }

                    if (state.showPremiumBottomSheet) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f)),
                        ) {
                            Spacer(
                                modifier =
                                    Modifier
                                        .fillMaxSize(0.2f)
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() },
                                        ) { onClosePremiumSheet() },
                            )

                            ServiceSubscriptionBottomSheet(
                                uiState = state.serviceSubscription,
                                loadService = onLoadServices,
                                onServiceSelected = onSelectService,
                                onTierSelected = onSelectTier,
                                onDismiss = onClosePremiumSheet,
                                onSubscribe = { _, tier ->
                                    onStartPayment(tier.getAmountInPaise().toLong())
                                    onClosePremiumSheet()
                                },
                                modifier = Modifier.align(Alignment.BottomCenter),
                            )
                        }
                    }
                },
                bottomBar = bottomBar,
            )

            NotificationToast(
                modifier = Modifier.align(Alignment.BottomCenter),
                message = stringResource(Res.string.premium_activated_msg_txt),
                title = stringResource(Res.string.premium_activated_title_txt),
                type = ToastType.SUCCESS,
                isVisible = state.showPremiumActivationToast,
                onDismiss = onDismissPremiumActivationToast,
                anchorState = toastAnchorState,
            )
        }
    }
}

private val enterFromBottom =
    fadeIn(animationSpec = tween(durationMillis = 500)) +
            slideInVertically(
                animationSpec = tween(durationMillis = 500),
                initialOffsetY = { it / 3 },
            )

internal object SettingsAnimationState {
    var hasPlayedIntroAnimation = false
}

// region Previews

private fun previewNoOpContent(
    paymentUiState: PaymentUiState = PaymentUiState(),
    isLoggingOut: Boolean = false,
    state: SettingsState = SettingsState(),
): @Composable () -> Unit =
    {
        SettingsScreenContent(
            paymentUiState = paymentUiState,
            isLoggingOut = isLoggingOut,
            versionName = "1.0.0",
            shouldShowNotificationItem = true,
            state = state,
            onLogout = {},
            onBackNavAction = {},
            onLanguageNavAction = {},
            onNotificationNavAction = {},
            onStartPayment = {},
            onBottomBarVisibilityChange = {},
            onOpenPremiumSheet = {},
            onClosePremiumSheet = {},
            onOpenLogoutDialog = {},
            onCloseLogoutDialog = {},
            onOpenWebUrl = {},
            onCloseWebView = {},
            onDismissPremiumActivationToast = {},
            onLoadServices = {},
            onSelectService = {},
            onSelectTier = {},
        )
    }

private val previewMonthlyTier =
    PricingTier(
        id = "tier-monthly",
        amount = 99,
        currency = "INR",
        duration = 1,
        durationType = DurationType.MONTHS,
        isDefault = true,
        isFeatured = false,
        displayOrder = 0,
    )

private val previewYearlyTier =
    PricingTier(
        id = "tier-yearly",
        amount = 899,
        currency = "INR",
        duration = 1,
        durationType = DurationType.YEARS,
        isDefault = false,
        isFeatured = true,
        displayOrder = 1,
    )

private val previewService =
    Service(
        id = "service-premium",
        serviceCode = "PREMIUM",
        displayName = "Weatherify Premium",
        description = "Unlock the full experience",
        pricingTiers = listOf(previewMonthlyTier, previewYearlyTier),
        features =
            listOf(
                Feature(id = "f1", description = "Ad-free experience", isHighlighted = true),
                Feature(id = "f2", description = "Hourly forecast up to 7 days"),
                Feature(id = "f3", description = "Severe weather alerts"),
            ),
        status = ServiceStatus.ACTIVE,
        limits = emptyMap(),
        createdAt = "",
        updatedAt = "",
    )

@Preview
@Composable
private fun SettingsScreenContentDefaultPreview() {
    MaterialTheme {
        Surface {
            previewNoOpContent()()
        }
    }
}

@Preview
@Composable
private fun SettingsScreenContentPremiumActivePreview() {
    MaterialTheme {
        Surface {
            previewNoOpContent(
                paymentUiState =
                    PaymentUiState(
                        isPremiumActivated = true,
                        expiryMillis = 1_800_000_000_000L,
                    ),
            )()
        }
    }
}

@Preview
@Composable
private fun SettingsScreenContentPremiumProcessingPreview() {
    MaterialTheme {
        Surface {
            previewNoOpContent(
                paymentUiState = PaymentUiState(stage = PaymentStage.AwaitingPayment, loading = true),
            )()
        }
    }
}

@Preview
@Composable
private fun SettingsScreenContentLogoutDialogPreview() {
    MaterialTheme {
        Surface {
            previewNoOpContent(state = SettingsState(showLogoutDialog = true))()
        }
    }
}

@Preview
@Composable
private fun SettingsScreenContentLoggingOutPreview() {
    MaterialTheme {
        Surface {
            previewNoOpContent(
                isLoggingOut = true,
                state = SettingsState(showLogoutDialog = true),
            )()
        }
    }
}

@Preview
@Composable
private fun SettingsScreenContentPremiumSheetLoadingPreview() {
    MaterialTheme {
        Surface {
            previewNoOpContent(
                state =
                    SettingsState(
                        showPremiumBottomSheet = true,
                        serviceSubscription = ServiceSubscriptionState(isLoading = true),
                    ),
            )()
        }
    }
}

@Preview
@Composable
private fun SettingsScreenContentPremiumSheetPlanSelectedPreview() {
    MaterialTheme {
        Surface {
            previewNoOpContent(
                state =
                    SettingsState(
                        showPremiumBottomSheet = true,
                        serviceSubscription =
                            ServiceSubscriptionState(
                                isLoading = false,
                                services = listOf(previewService),
                                selectedService = previewService,
                                selectedTier = previewYearlyTier,
                            ),
                    ),
            )()
        }
    }
}

@Preview
@Composable
private fun SettingsScreenContentPremiumSheetErrorPreview() {
    MaterialTheme {
        Surface {
            previewNoOpContent(
                state =
                    SettingsState(
                        showPremiumBottomSheet = true,
                        serviceSubscription =
                            ServiceSubscriptionState(
                                isLoading = false,
                                error = "Something went wrong. Please try again.",
                            ),
                    ),
            )()
        }
    }
}

@Preview
@Composable
private fun SettingsScreenContentPremiumActivationToastPreview() {
    MaterialTheme {
        Surface {
            previewNoOpContent(state = SettingsState(showPremiumActivationToast = true))()
        }
    }
}

// endregion
