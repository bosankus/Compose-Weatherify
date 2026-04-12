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
import bose.ankush.commonui.components.PremiumBottomSheetContent
import bose.ankush.commonui.components.ToastAnchorState
import bose.ankush.commonui.util.formatDate
import bose.ankush.commonui.web.InAppWebView
import bose.ankush.payment.presentation.PaymentStage
import bose.ankush.payment.presentation.PaymentUiState
import kotlinx.coroutines.delay

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
    toastAnchorState: ToastAnchorState? = null,
    bottomBar: @Composable () -> Unit = {}
) {
    val showPremiumBottomSheet = remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState()
    val currentWebUrl = remember { mutableStateOf<String?>(null) }
    val showLogoutDialog = remember { mutableStateOf(false) }

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
                    title = { Text("Profile", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBackNavAction) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back"
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
                            onClick = { showPremiumBottomSheet.value = true }
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
                                onLanguageNavAction = { onLanguageNavAction(languageList) }
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
                                currentWebUrl = currentWebUrl
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
                                    text = "Logout",
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
                        title = { Text(text = "Logout") },
                        text = { Text(text = "Are you sure you want to logout?") },
                        confirmButton = {
                            TextButton(
                                onClick = onLogout,
                                enabled = !isLoggingOut
                            ) {
                                Text("Confirm")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showLogoutDialog.value = false },
                                enabled = !isLoggingOut
                            ) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                if (showPremiumBottomSheet.value) {
                    ModalBottomSheet(
                        onDismissRequest = { showPremiumBottomSheet.value = false },
                        sheetState = bottomSheetState,
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        PremiumBottomSheetContent(
                            isLoading = paymentUiState.loading,
                            onDismiss = { showPremiumBottomSheet.value = false },
                            onSubscribe = onStartPayment
                        )
                    }
                }
            },
            bottomBar = bottomBar
        )
    }
}

@Composable
fun PremiumCard(
    paymentUiState: PaymentUiState,
    onClick: () -> Unit
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
            SubscribedPremiumCard(paymentUiState)
        } else {
            UnsubscribedPremiumCard(paymentUiState, onClick)
        }
    }
}

@Composable
fun UnsubscribedPremiumCard(
    paymentUiState: PaymentUiState,
    onClick: () -> Unit
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
            text = if (isLoading) "Processing\u2026" else "Get Premium",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isLoading) "Please wait while we activate your premium subscription."
            else "Unlock all features and enjoy an ad-free experience.",
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
            Text(if (isLoading) "Processing\u2026" else "Upgrade Now")
        }
    }
}

@Composable
fun SubscribedPremiumCard(paymentUiState: PaymentUiState) {
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
            text = "You are a Premium User",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.height(8.dp))
        val expiryTop = paymentUiState.expiryMillis
        if (expiryTop != null) {
            val dateStr = remember(expiryTop) { formatDate(expiryTop) }
            Text(
                text = "Expires $dateStr",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
            )
        } else {
            Text(
                text = "Active",
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
                title = "Notifications",
                onClick = onNotificationNavAction
            )
        }
        SettingsItem(
            icon = Icons.Outlined.Language,
            title = "Language",
            onClick = onLanguageNavAction
        )
    }
}

@Composable
fun LegalSection(
    versionName: String,
    currentWebUrl: MutableState<String?>
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
            title = "Privacy Policy",
            onClick = { currentWebUrl.value = "https://data.androidplay.in/wfy/privacy-policy" }
        )
        SettingsItem(
            icon = Icons.Outlined.Gavel,
            title = "Terms of Use",
            onClick = {
                currentWebUrl.value = "https://data.androidplay.in/wfy/terms-and-conditions"
            }
        )
        SettingsItem(
            icon = Icons.Outlined.Info,
            title = "App Version",
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
                contentDescription = "Navigate to next screen",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
