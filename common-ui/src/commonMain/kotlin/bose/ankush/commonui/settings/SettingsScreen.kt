package bose.ankush.commonui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import bose.ankush.commonui.components.NotificationToast
import bose.ankush.commonui.components.PremiumBottomSheetContent
import bose.ankush.commonui.components.PremiumBottomSheetStrings
import bose.ankush.commonui.components.ToastAnchorState
import bose.ankush.commonui.components.ToastType
import bose.ankush.commonui.util.formatDate
import bose.ankush.commonui.web.InAppWebView
import bose.ankush.payment.presentation.PaymentStage
import bose.ankush.payment.presentation.PaymentUiState
import kotlinx.coroutines.delay

/**
 * Holds localized strings for SettingsScreen.
 * Allows the KMP composable to accept platform-specific localized resources.
 */
data class SettingsScreenStrings(
    val profileTitle: String = "Profile",
    val logout: String = "Logout",
    val logoutConfirmation: String = "Are you sure you want to logout?",
    val confirm: String = "Confirm",
    val cancel: String = "Cancel",
    val getPremium: String = "Get Premium",
    val processing: String = "Processing…",
    val processingDescription: String = "Please wait while we activate your premium subscription.",
    val unlockDescription: String = "Unlock all features and enjoy an ad-free experience.",
    val upgradeNow: String = "Upgrade Now",
    val premiumActive: String = "You are a Premium User",
    val premiumExpires: String = "Expires %s",
    val premiumActiveStatus: String = "Active",
    val notificationsTitle: String = "Notifications",
    val languageTitle: String = "Language",
    val privacyPolicy: String = "Privacy Policy",
    val termsOfUse: String = "Terms of Use",
    val appVersion: String = "App Version",
    val backButtonDesc: String = "Back",
    val arrowRightDesc: String = "Navigate to next screen"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    paymentUiState: PaymentUiState,
    isLoggingOut: Boolean,
    isLoggedOut: Boolean,
    versionName: String,
    shouldShowNotificationItem: Boolean,
    languageList: Array<String>,
    onLogout: () -> Unit,
    onLoggedOutHandled: () -> Unit,
    onStartPayment: () -> Unit,
    onBackNavAction: () -> Unit,
    onLanguageNavAction: (Array<String>) -> Unit,
    onNotificationNavAction: () -> Unit,
    strings: SettingsScreenStrings = SettingsScreenStrings(),
    premiumStrings: PremiumBottomSheetStrings? = null,
    toastAnchorState: ToastAnchorState? = null,
    bottomBar: @Composable () -> Unit = {}
) {
    val showPremiumBottomSheet = remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState()
    val currentWebUrl = remember { mutableStateOf<String?>(null) }
    val showLogoutDialog = remember { mutableStateOf(false) }
    val showPremiumActivationToast = remember { mutableStateOf(false) }

    LaunchedEffect(paymentUiState.stage) {
        if (paymentUiState.stage == PaymentStage.Success) {
            showPremiumActivationToast.value = true
            showPremiumBottomSheet.value = false
        }
    }

    LaunchedEffect(isLoggedOut) {
        if (isLoggedOut) {
            showLogoutDialog.value = false
            onLoggedOutHandled()
        }
    }

    val settingsSectionState = remember { MutableTransitionState(false) }
    val legalSectionState = remember { MutableTransitionState(false) }
    val logoutButtonState = remember { MutableTransitionState(false) }

    LaunchedEffect(Unit) {
        settingsSectionState.targetState = false
        legalSectionState.targetState = false
        logoutButtonState.targetState = false

        delay(100)
        settingsSectionState.targetState = true
        delay(150)
        legalSectionState.targetState = true
        delay(150)
        logoutButtonState.targetState = true
    }

    if (currentWebUrl.value != null) {
        InAppWebView(
            url = currentWebUrl.value!!,
            onClose = { currentWebUrl.value = null }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(strings.profileTitle, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBackNavAction) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = strings.backButtonDesc
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            content = { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                ) {
                    // Future enhancement: Add user profile section here

                    item { Spacer(modifier = Modifier.height(24.dp)) }

                    item {
                        PremiumCard(
                            paymentUiState = paymentUiState,
                            onClick = { showPremiumBottomSheet.value = true },
                            strings = strings
                        )
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }

                    item {
                        AnimatedVisibility(
                            visibleState = settingsSectionState,
                            enter = fadeIn(animationSpec = tween(durationMillis = 500)) +
                                    slideInVertically(
                                        animationSpec = tween(durationMillis = 500),
                                        initialOffsetY = { it / 3 }
                                    ),
                            exit = fadeOut()
                        ) {
                            SettingsSection(
                                shouldShowNotificationItem = shouldShowNotificationItem,
                                onNotificationNavAction = onNotificationNavAction,
                                onLanguageNavAction = { onLanguageNavAction(languageList) },
                                strings = strings
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }

                    item {
                        AnimatedVisibility(
                            visibleState = legalSectionState,
                            enter = fadeIn(animationSpec = tween(durationMillis = 500)) +
                                    slideInVertically(
                                        animationSpec = tween(durationMillis = 500),
                                        initialOffsetY = { it / 3 }
                                    ),
                            exit = fadeOut()
                        ) {
                            LegalSection(
                                versionName = versionName,
                                currentWebUrl = currentWebUrl,
                                strings = strings
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }

                    item {
                        AnimatedVisibility(
                            visibleState = logoutButtonState,
                            enter = fadeIn(animationSpec = tween(durationMillis = 500)) +
                                    slideInVertically(
                                        animationSpec = tween(durationMillis = 500),
                                        initialOffsetY = { it / 3 }
                                    ),
                            exit = fadeOut()
                        ) {
                            TextButton(
                                onClick = { showLogoutDialog.value = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = strings.logout,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }

                if (showLogoutDialog.value) {
                    AlertDialog(
                        onDismissRequest = { if (!isLoggingOut) showLogoutDialog.value = false },
                        title = { Text(text = strings.logout) },
                        text = { Text(text = strings.logoutConfirmation) },
                        confirmButton = {
                            TextButton(
                                onClick = onLogout,
                                enabled = !isLoggingOut
                            ) {
                                Text(strings.confirm)
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showLogoutDialog.value = false },
                                enabled = !isLoggingOut
                            ) {
                                Text(strings.cancel)
                            }
                        }
                    )
                }

                if (showPremiumBottomSheet.value && premiumStrings != null) {
                    ModalBottomSheet(
                        onDismissRequest = { showPremiumBottomSheet.value = false },
                        sheetState = bottomSheetState,
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        PremiumBottomSheetContent(
                            strings = premiumStrings,
                            isLoading = paymentUiState.loading,
                            onDismiss = { showPremiumBottomSheet.value = false },
                            onSubscribe = onStartPayment
                        )
                    }
                }
            },
            bottomBar = bottomBar
        )

        NotificationToast(
            message = "Your premium subscription is now active!",
            title = "Premium Activated",
            type = ToastType.SUCCESS,
            isVisible = showPremiumActivationToast.value,
            onDismiss = { showPremiumActivationToast.value = false },
            anchorState = toastAnchorState
        )
    }
}

@Composable
fun PremiumCard(
    paymentUiState: PaymentUiState,
    onClick: () -> Unit,
    strings: SettingsScreenStrings = SettingsScreenStrings()
) {
    val isPremiumActive =
        paymentUiState.isPremiumActivated || paymentUiState.stage == PaymentStage.Success
    val cardColors = if (isPremiumActive) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!isPremiumActive) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = cardColors
    ) {
        if (isPremiumActive) {
            SubscribedPremiumCard(paymentUiState, strings)
        } else {
            UnsubscribedPremiumCard(paymentUiState, onClick, strings)
        }
    }
}

@Composable
fun UnsubscribedPremiumCard(
    paymentUiState: PaymentUiState,
    onClick: () -> Unit,
    strings: SettingsScreenStrings = SettingsScreenStrings()
) {
    val loadingStages = remember {
        listOf(
            PaymentStage.CreatingOrder,
            PaymentStage.AwaitingPayment,
            PaymentStage.Verifying
        )
    }
    val isLoading = paymentUiState.loading || paymentUiState.stage in loadingStages

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        Icon(
            imageVector = Icons.Outlined.WorkspacePremium,
            contentDescription = "Premium subscription icon",
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onTertiaryContainer
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isLoading) strings.processing else strings.getPremium,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isLoading) strings.processingDescription
            else strings.unlockDescription,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onClick,
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            )
        ) {
            Text(if (isLoading) strings.processing else strings.upgradeNow)
        }
    }
}

@Composable
fun SubscribedPremiumCard(
    paymentUiState: PaymentUiState,
    strings: SettingsScreenStrings = SettingsScreenStrings()
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.WorkspacePremium,
            contentDescription = "Premium subscription icon",
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = strings.premiumActive,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.height(8.dp))
        val expiryTop = paymentUiState.expiryMillis
        if (expiryTop != null) {
            val dateStr = remember(expiryTop) { formatDate(expiryTop) }
            Text(
                text = strings.premiumExpires
                    .replace("%1\$s", dateStr)
                    .replace("%s", dateStr),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
            )
        } else {
            Text(
                text = strings.premiumActiveStatus,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SettingsSection(
    shouldShowNotificationItem: Boolean,
    onNotificationNavAction: () -> Unit,
    onLanguageNavAction: () -> Unit,
    strings: SettingsScreenStrings = SettingsScreenStrings()
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(vertical = 8.dp)
    ) {
        if (shouldShowNotificationItem) {
            SettingsItem(
                icon = Icons.Outlined.Notifications,
                title = strings.notificationsTitle,
                onClick = onNotificationNavAction
            )
        }
        SettingsItem(
            icon = Icons.Outlined.Language,
            title = strings.languageTitle,
            onClick = onLanguageNavAction
        )
    }
}

@Composable
fun LegalSection(
    versionName: String,
    currentWebUrl: MutableState<String?>,
    strings: SettingsScreenStrings = SettingsScreenStrings()
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(vertical = 8.dp)
    ) {
        SettingsItem(
            icon = Icons.Outlined.PrivacyTip,
            title = strings.privacyPolicy,
            onClick = { currentWebUrl.value = "https://data.androidplay.in/wfy/privacy-policy" }
        )
        SettingsItem(
            icon = Icons.Outlined.Gavel,
            title = strings.termsOfUse,
            onClick = {
                currentWebUrl.value = "https://data.androidplay.in/wfy/terms-and-conditions"
            }
        )
        SettingsItem(
            icon = Icons.Outlined.Info,
            title = strings.appVersion,
            trailingContent = {
                Text(
                    text = versionName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        )
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailingContent()
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = SettingsScreenStrings().arrowRightDesc,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
