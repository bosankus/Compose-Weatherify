package bose.ankush.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bose.ankush.commonui.components.NotificationToast
import bose.ankush.commonui.components.ToastAnchorState
import bose.ankush.commonui.components.ToastType
import bose.ankush.commonui.theme.LightSystemBarIcons
import bose.ankush.commonui.theme.WarningYellow
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
import bose.ankush.settings.generated.resources.profile_remove_photo_body_txt
import bose.ankush.settings.generated.resources.profile_remove_photo_title_txt
import bose.ankush.settings.generated.resources.profile_remove_photo_txt
import bose.ankush.settings.generated.resources.profile_title
import bose.ankush.settings.presentation.component.AboutSection
import bose.ankush.settings.presentation.component.PreferencesSection
import bose.ankush.settings.presentation.component.PremiumCard
import bose.ankush.settings.presentation.component.ProfileHeader
import bose.ankush.settings.presentation.component.ProfileHeaderActions
import bose.ankush.settings.presentation.component.ProfilePhotoOptionsSheet
import bose.ankush.settings.presentation.component.SettingsBackdrop
import bose.ankush.settings.presentation.component.SettingsCardShape
import bose.ankush.settings.presentation.component.SettingsDanger
import bose.ankush.settings.presentation.component.SettingsInk
import bose.ankush.settings.presentation.component.SettingsInkMuted
import bose.ankush.settings.presentation.component.SettingsSurface
import bose.ankush.settings.presentation.profile.PhotoOperation
import bose.ankush.settings.presentation.profile.ProfileIntent
import bose.ankush.settings.presentation.profile.ProfileMessage
import bose.ankush.settings.presentation.profile.ProfileState
import bose.ankush.settings.presentation.profile.ProfileViewModel
import bose.ankush.settings.presentation.profile.fullText
import bose.ankush.settings.presentation.profile.isError
import bose.ankush.settings.presentation.profile.rememberProfilePhotoPicker
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Public entry point for the Settings feature. Bridges to the host app via plain
 * callbacks/params. Pull to refresh refetches the account here and calls [onRefreshPremium],
 * which the host wires to its session refresh, the only call that returns premium status.
 * [SettingsViewModel] and [ProfileViewModel] stay internal, resolved through
 * Koin and scoped to wherever this route is composed.
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
    onRefreshPremium: () -> Unit = {},
    toastAnchorState: ToastAnchorState? = null,
    bottomBar: @Composable () -> Unit = {},
) {
    val viewModel = koinViewModel<SettingsViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val profileViewModel = koinViewModel<ProfileViewModel>()
    val profileState by profileViewModel.state.collectAsStateWithLifecycle()
    var profileMessage by remember { mutableStateOf<ProfileMessage?>(null) }
    val previousPaymentStage = remember { mutableStateOf(paymentUiState.stage) }

    val photoPicker =
        rememberProfilePhotoPicker(
            onPicked = { profileViewModel.onIntent(ProfileIntent.PhotoPicked(it)) },
            onUnreadable = { profileViewModel.onIntent(ProfileIntent.PhotoUnreadable) },
        )

    LaunchedEffect(profileViewModel) {
        profileViewModel.onIntent(ProfileIntent.Refresh)
        profileViewModel.messages.collect { profileMessage = it }
    }

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
        model =
            SettingsScreenModel(
                state = state,
                profile =
                    ProfileSection(
                        state = profileState,
                        actions =
                            ProfileHeaderActions(
                                onChangePhoto =
                                    photoPicker?.let { picker ->
                                        {
                                            profileViewModel.onIntent(ProfileIntent.DismissPhotoOptions)
                                            picker.launch()
                                        }
                                    },
                                onOpenPhotoOptions = { profileViewModel.onIntent(ProfileIntent.OpenPhotoOptions) },
                            ),
                        message = profileMessage,
                        onRequestRemove = { profileViewModel.onIntent(ProfileIntent.RequestRemovePhoto) },
                        onDismissPhotoOptions = { profileViewModel.onIntent(ProfileIntent.DismissPhotoOptions) },
                        onConfirmRemove = { profileViewModel.onIntent(ProfileIntent.ConfirmRemovePhoto) },
                        onDismissRemove = { profileViewModel.onIntent(ProfileIntent.DismissRemovePhoto) },
                        onDismissMessage = { profileMessage = null },
                    ),
                paymentUiState = paymentUiState,
                isLoggingOut = isLoggingOut,
                versionName = versionName,
                shouldShowNotificationItem = shouldShowNotificationItem,
            ),
        actions =
            SettingsActions(
                onRefresh = {
                    profileViewModel.onIntent(ProfileIntent.PullToRefresh)
                    onRefreshPremium()
                },
                onLogout = onLogout,
                onBack = onBackNavAction,
                onLanguage = { onLanguageNavAction(languageList) },
                onNotifications = onNotificationNavAction,
                onBottomBarVisibilityChange = onBottomBarVisibilityChange,
                onOpenLogoutDialog = { viewModel.processIntent(SettingsIntent.OpenLogoutDialog) },
                onCloseLogoutDialog = { viewModel.processIntent(SettingsIntent.CloseLogoutDialog) },
                onOpenWebUrl = { url -> viewModel.processIntent(SettingsIntent.OpenWebUrl(url)) },
                onCloseWebView = { viewModel.processIntent(SettingsIntent.CloseWebView) },
                premium =
                    PremiumActions(
                        onOpenSheet = { viewModel.processIntent(SettingsIntent.OpenPremiumSheet) },
                        onCloseSheet = { viewModel.processIntent(SettingsIntent.ClosePremiumSheet) },
                        onLoadServices = { viewModel.processIntent(SettingsIntent.LoadServices) },
                        onSelectService = { viewModel.processIntent(SettingsIntent.SelectService(it)) },
                        onSelectTier = { viewModel.processIntent(SettingsIntent.SelectTier(it)) },
                        onStartPayment = onStartPayment,
                        onDismissActivationToast = {
                            viewModel.processIntent(SettingsIntent.DismissPremiumActivationToast)
                        },
                    ),
            ),
        toastAnchorState = toastAnchorState,
        bottomBar = bottomBar,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreenContent(
    model: SettingsScreenModel,
    actions: SettingsActions,
    toastAnchorState: ToastAnchorState? = null,
    bottomBar: @Composable () -> Unit = {},
) {
    val state = model.state
    val profile = model.profile
    LaunchedEffect(state.showPremiumBottomSheet) {
        actions.onBottomBarVisibilityChange(!state.showPremiumBottomSheet)
    }

    val currentWebUrl = state.currentWebUrl
    if (currentWebUrl != null) {
        InAppWebView(url = currentWebUrl, onClose = actions.onCloseWebView)
        return
    }

    LightSystemBarIcons()
    Box(modifier = Modifier.fillMaxSize().background(SettingsBackdrop)) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentColor = SettingsInk,
            topBar = { SettingsTopBar(onBackNavAction = actions.onBack) },
            bottomBar = bottomBar,
        ) { innerPadding ->
            val pullState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = profile.state.isRefreshing,
                onRefresh = actions.onRefresh,
                state = pullState,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullState,
                        isRefreshing = profile.state.isRefreshing,
                        containerColor = SettingsSurface,
                        color = WarningYellow,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                },
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    item { ProfileHeader(state = profile.state, actions = profile.actions) }
                    item {
                        PremiumCard(
                            paymentUiState = model.paymentUiState,
                            onClick = actions.premium.onOpenSheet,
                        )
                    }
                    item {
                        PreferencesSection(
                            shouldShowNotificationItem = model.shouldShowNotificationItem,
                            onNotificationNavAction = actions.onNotifications,
                            onLanguageNavAction = actions.onLanguage,
                        )
                    }
                    item {
                        AboutSection(
                            versionName = model.versionName,
                            onUrlClick = actions.onOpenWebUrl,
                        )
                    }
                    item {
                        TextButton(
                            onClick = actions.onOpenLogoutDialog,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(Res.string.logout_btn_txt),
                                color = SettingsDanger,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }

            if (state.showLogoutDialog) {
                SettingsConfirmDialog(
                    copy =
                        ConfirmCopy(
                            title = stringResource(Res.string.logout_btn_txt),
                            body = stringResource(Res.string.logout_confirmation_txt),
                            confirm = stringResource(Res.string.confirm_btn_txt),
                        ),
                    enabled = !model.isLoggingOut,
                    onConfirm = actions.onLogout,
                    onDismiss = actions.onCloseLogoutDialog,
                )
            }

            val changePhoto = profile.actions.onChangePhoto
            if (profile.state.isPhotoOptionsVisible && changePhoto != null) {
                ProfilePhotoOptionsSheet(
                    onChangePhoto = changePhoto,
                    onRemovePhoto = profile.onRequestRemove,
                    onDismiss = profile.onDismissPhotoOptions,
                )
            }

            if (profile.state.isRemoveConfirmVisible) {
                SettingsConfirmDialog(
                    copy =
                        ConfirmCopy(
                            title = stringResource(Res.string.profile_remove_photo_title_txt),
                            body = stringResource(Res.string.profile_remove_photo_body_txt),
                            confirm = stringResource(Res.string.profile_remove_photo_txt),
                        ),
                    onConfirm = profile.onConfirmRemove,
                    onDismiss = profile.onDismissRemove,
                )
            }

            if (state.showPremiumBottomSheet) {
                PremiumSheetOverlay(state = state, actions = actions)
            }
        }

        NotificationToast(
            modifier = Modifier.align(Alignment.BottomCenter),
            message = stringResource(Res.string.premium_activated_msg_txt),
            title = stringResource(Res.string.premium_activated_title_txt),
            type = ToastType.SUCCESS,
            isVisible = state.showPremiumActivationToast,
            onDismiss = actions.premium.onDismissActivationToast,
            anchorState = toastAnchorState,
        )

        val message = profile.message
        NotificationToast(
            modifier = Modifier.align(Alignment.BottomCenter),
            message = message?.fullText().orEmpty(),
            title = stringResource(Res.string.profile_title),
            type = if (message?.isError() == true) ToastType.ERROR else ToastType.SUCCESS,
            isVisible = message != null,
            onDismiss = profile.onDismissMessage,
            anchorState = toastAnchorState,
        )
    }
}

@Composable
private fun SettingsTopBar(onBackNavAction: () -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        IconButton(onClick = onBackNavAction) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(Res.string.back_button_content),
                tint = SettingsInk,
            )
        }
    }
}

@Composable
private fun SettingsConfirmDialog(
    copy: ConfirmCopy,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    enabled: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = { if (enabled) onDismiss() },
        containerColor = SettingsSurface,
        titleContentColor = SettingsInk,
        textContentColor = SettingsInkMuted,
        shape = SettingsCardShape,
        title = { Text(text = copy.title, fontWeight = FontWeight.SemiBold) },
        text = { Text(text = copy.body) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = enabled) {
                Text(
                    copy.confirm,
                    color = SettingsDanger.copy(alpha = if (enabled) 1f else DISABLED_ALPHA),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = enabled) {
                Text(stringResource(Res.string.cancel_btn_txt), color = SettingsInk)
            }
        },
    )
}

@Composable
private fun PremiumSheetOverlay(
    state: SettingsState,
    actions: SettingsActions,
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = SCRIM_ALPHA))) {
        Spacer(
            modifier =
                Modifier
                    .fillMaxSize(0.2f)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { actions.premium.onCloseSheet() },
        )
        ServiceSubscriptionBottomSheet(
            uiState = state.serviceSubscription,
            loadService = actions.premium.onLoadServices,
            onServiceSelected = actions.premium.onSelectService,
            onTierSelected = actions.premium.onSelectTier,
            onDismiss = actions.premium.onCloseSheet,
            onSubscribe = { _, tier ->
                actions.premium.onStartPayment(tier.getAmountInPaise().toLong())
                actions.premium.onCloseSheet()
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private const val SCRIM_ALPHA = 0.5f
private const val DISABLED_ALPHA = 0.4f

// region Previews

private fun previewNoOpContent(
    paymentUiState: PaymentUiState = PaymentUiState(),
    isLoggingOut: Boolean = false,
    state: SettingsState = SettingsState(),
    profile: ProfileState = ProfileState(email = "ankush@example.com"),
): @Composable () -> Unit =
    {
        SettingsScreenContent(
            model =
                SettingsScreenModel(
                    state = state,
                    profile =
                        ProfileSection(
                            state = profile,
                            actions = ProfileHeaderActions(
                                onChangePhoto = {},
                                onOpenPhotoOptions = {}),
                        ),
                    paymentUiState = paymentUiState,
                    isLoggingOut = isLoggingOut,
                    versionName = "1.0.0",
                    shouldShowNotificationItem = true,
                ),
            actions = SettingsActions(),
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

@Preview
@Composable
private fun SettingsScreenContentPhotoUploadingPreview() {
    MaterialTheme {
        Surface {
            previewNoOpContent(
                profile =
                    ProfileState(
                        email = "ankush@example.com",
                        photoUrl = "https://example.com/photo.jpg",
                        photoOperation = PhotoOperation.Uploading,
                    ),
            )()
        }
    }
}
