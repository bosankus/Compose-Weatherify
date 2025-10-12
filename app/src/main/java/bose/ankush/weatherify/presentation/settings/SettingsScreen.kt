package bose.ankush.weatherify.presentation.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import bose.ankush.sunriseui.premium.PremiumBottomSheetContent
import bose.ankush.weatherify.BuildConfig
import bose.ankush.weatherify.base.LocaleConfigMapper
import bose.ankush.weatherify.base.common.Extension.openUrlInBrowser
import bose.ankush.weatherify.presentation.AuthState
import bose.ankush.weatherify.presentation.MainViewModel
import bose.ankush.weatherify.presentation.navigation.AppBottomBar
import bose.ankush.weatherify.presentation.payment.PaymentStage
import bose.ankush.weatherify.presentation.payment.PaymentUiState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    viewModel: MainViewModel,
    navController: NavController,
    onLanguageNavAction: (Array<String>) -> Unit,
    onNotificationNavAction: () -> Unit
) {
    val languageList = LocaleConfigMapper.getAvailableLanguagesFromJson(
        jsonFile = "countryConfig.json",
        context = LocalContext.current
    )

    // State for Premium bottom sheet
    val showPremiumBottomSheet = remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState()

    val paymentUiState = viewModel.paymentUiState.collectAsState().value

    // Logout dialog state
    val showLogoutDialog = remember { mutableStateOf(false) }

    // Observe auth state to reflect logout loading/success
    val authState = viewModel.authState.collectAsState().value
    val isLoggingOut = authState is AuthState.LogoutLoading

    // Close dialog on successful logout (token cleared -> MainActivity shows Login)
    LaunchedEffect(authState) {
        if (authState is AuthState.LoggedOut) {
            showLogoutDialog.value = false
            // Reset to avoid lingering LoggedOut state
            viewModel.resetAuthState()
        }
    }

    // Animation states for screen components
    val settingsSectionState = remember { MutableTransitionState(false) }
    val legalSectionState = remember { MutableTransitionState(false) }
    val logoutButtonState = remember { MutableTransitionState(false) }

    LaunchedEffect(Unit) {
        // Reset and start staggered animations to align with app style
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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

                // Settings Section
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
                            onNotificationNavAction = onNotificationNavAction,
                            onLanguageNavAction = { onLanguageNavAction(languageList) }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                // Legal Section
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
                        LegalSection()
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                // Logout Button
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
                            onClick = { viewModel.logout() },
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

            // Premium Bottom Sheet
            if (showPremiumBottomSheet.value) {
                ModalBottomSheet(
                    onDismissRequest = { showPremiumBottomSheet.value = false },
                    sheetState = bottomSheetState,
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    PremiumBottomSheetContent(
                        onDismiss = { showPremiumBottomSheet.value = false },
                        onSubscribe = {
                            showPremiumBottomSheet.value = false
                            viewModel.startPayment()
                        }
                    )
                }
            }
        },
        bottomBar = {
            AppBottomBar(
                isVisible = rememberSaveable { mutableStateOf(true) },
                navController = navController
            )
        }
    )
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
            UnsubscribedPremiumCard(onClick)
        }
    }
}

@Composable
fun UnsubscribedPremiumCard(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.WorkspacePremium,
            contentDescription = "Premium",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onTertiaryContainer
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Get Premium",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Unlock all features and enjoy an ad-free experience.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            )
        ) {
            Text("Upgrade Now")
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
            contentDescription = "Premium",
            modifier = Modifier.size(48.dp),
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
            val df = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            val dateStr = df.format(Date(expiryTop))
            Text(
                text = "Expires $dateStr",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
            )
        } else {
            Text(
                text = "Active",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF2E7D32),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SettingsSection(
    onNotificationNavAction: () -> Unit,
    onLanguageNavAction: () -> Unit,
) {
    val context = LocalContext.current

    // Determine whether to show notification permission item.
    val shouldShowNotificationItem = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
    } else {
        // For API < 33, permission is not required, so don't show the item.
        false
    }

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
fun LegalSection() {
    val context = LocalContext.current
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
            onClick = { context.openUrlInBrowser("https://data.androidplay.in/wfy/privacy-policy") }
        )
        SettingsItem(
            icon = Icons.Outlined.Gavel,
            title = "Terms of Use",
            onClick = { context.openUrlInBrowser("https://data.androidplay.in/wfy/terms-and-conditions") }
        )
        SettingsItem(
            icon = Icons.Outlined.Info,
            title = "App Version",
            trailingContent = {
                Text(
                    text = BuildConfig.VERSION_NAME,
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
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (trailingContent != null) {
            trailingContent()
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
