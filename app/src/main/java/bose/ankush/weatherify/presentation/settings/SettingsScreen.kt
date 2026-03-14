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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.widget.Toast
import bose.ankush.weatherify.R
import bose.ankush.weatherify.presentation.ui.Dimensions
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
    val context = LocalContext.current
    val errorMessage = stringResource(R.string.locale_config_error_txt)
    val languageList = remember(context) {
        try {
            LocaleConfigMapper.getAvailableLanguagesFromJson(
                jsonFile = "countryConfig.json",
                context = context
            )
        } catch (_: Exception) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            emptyArray()
        }
    }

    // State for Premium bottom sheet
    val showPremiumBottomSheet = remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState()

    val paymentUiState = viewModel.paymentUiState.collectAsState().value

    // Logout dialog state
    val showLogoutDialog = remember { mutableStateOf(false) }

    // Memoized callbacks
    val onShowPremiumSheet = remember { { showPremiumBottomSheet.value = true } }
    val onShowLogoutDialog = remember { { showLogoutDialog.value = true } }

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

        delay(Dimensions.Animation.STAGGER_DELAY_SMALL.toLong())
        settingsSectionState.targetState = true
        delay(Dimensions.Animation.STAGGER_DELAY_MEDIUM.toLong())
        legalSectionState.targetState = true
        delay(Dimensions.Animation.STAGGER_DELAY_MEDIUM.toLong())
        logoutButtonState.targetState = true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.profile_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back_button_content)
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
                    .padding(horizontal = Dimensions.screen_horizontal_padding)
            ) {
                // Future enhancement: Add user profile section here

                item { Spacer(modifier = Modifier.height(Dimensions.spacing_24)) }

                item {
                    PremiumCard(
                        paymentUiState = paymentUiState,
                        onClick = onShowPremiumSheet
                    )
                }

                item { Spacer(modifier = Modifier.height(Dimensions.spacing_24)) }

                // Settings Section
                item {
                    AnimatedVisibility(
                        visibleState = settingsSectionState,
                        enter = fadeIn(animationSpec = tween(durationMillis = Dimensions.Animation.SLOW)) +
                                slideInVertically(
                                    animationSpec = tween(durationMillis = Dimensions.Animation.SLOW),
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

                item { Spacer(modifier = Modifier.height(Dimensions.spacing_24)) }

                // Legal Section
                item {
                    AnimatedVisibility(
                        visibleState = legalSectionState,
                        enter = fadeIn(animationSpec = tween(durationMillis = Dimensions.Animation.SLOW)) +
                                slideInVertically(
                                    animationSpec = tween(durationMillis = Dimensions.Animation.SLOW),
                                    initialOffsetY = { it / 3 }
                                ),
                        exit = fadeOut()
                    ) {
                        LegalSection()
                    }
                }

                item { Spacer(modifier = Modifier.height(Dimensions.spacing_24)) }

                // Logout Button
                item {
                    AnimatedVisibility(
                        visibleState = logoutButtonState,
                        enter = fadeIn(animationSpec = tween(durationMillis = Dimensions.Animation.SLOW)) +
                                slideInVertically(
                                    animationSpec = tween(durationMillis = Dimensions.Animation.SLOW),
                                    initialOffsetY = { it / 3 }
                                ),
                        exit = fadeOut()
                    ) {
                        TextButton(
                            onClick = onShowLogoutDialog,
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

                item { Spacer(modifier = Modifier.height(Dimensions.spacing_24)) }
            }

            if (showLogoutDialog.value) {
                AlertDialog(
                    onDismissRequest = { if (!isLoggingOut) showLogoutDialog.value = false },
                    title = { Text(stringResource(R.string.logout_btn_txt)) },
                    text = {
                        Column {
                            Text(stringResource(R.string.logout_confirmation_txt))
                            Spacer(modifier = Modifier.height(Dimensions.spacing_8))
                            Text(
                                text = stringResource(R.string.logout_warning_txt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    },
                    confirmButton = {
                        if (isLoggingOut) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(Dimensions.icon_default),
                                strokeWidth = 2.dp
                            )
                        } else {
                            TextButton(
                                onClick = { viewModel.logout() }
                            ) {
                                Text(stringResource(R.string.confirm_btn_txt))
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showLogoutDialog.value = false },
                            enabled = !isLoggingOut
                        ) {
                            Text(stringResource(R.string.cancel_btn_txt))
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
                isVisible = remember { mutableStateOf(true) },
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
    val context = LocalContext.current
    val isPremiumActive =
        paymentUiState.isPremiumActivated || paymentUiState.stage == PaymentStage.Success
    val cardColors = if (isPremiumActive) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    }

    // Show toast on successful payment
    val toastMessage = stringResource(R.string.premium_activated_txt)
    LaunchedEffect(paymentUiState.stage) {
        if (paymentUiState.stage == PaymentStage.Success) {
            Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
        }
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
            .padding(Dimensions.spacing_16),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(Dimensions.icon_xlarge),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.WorkspacePremium,
                contentDescription = stringResource(R.string.premium_icon_content),
                modifier = Modifier.size(Dimensions.icon_xlarge),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
        Spacer(modifier = Modifier.height(Dimensions.spacing_16))
        Text(
            text = stringResource(if (isLoading) R.string.premium_processing_txt else R.string.premium_get_txt),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
        Spacer(modifier = Modifier.height(Dimensions.spacing_8))
        Text(
            text = stringResource(if (isLoading) R.string.premium_processing_desc_txt else R.string.premium_unlock_desc_txt),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Dimensions.spacing_16))
        Button(
            onClick = onClick,
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            )
        ) {
            Text(stringResource(if (isLoading) R.string.premium_processing_txt else R.string.premium_upgrade_btn_txt))
        }
    }
}

@Composable
fun SubscribedPremiumCard(paymentUiState: PaymentUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimensions.spacing_16),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.WorkspacePremium,
            contentDescription = stringResource(R.string.premium_icon_content),
            modifier = Modifier.size(Dimensions.icon_xlarge),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.height(Dimensions.spacing_16))
        Text(
            text = stringResource(R.string.premium_active_txt),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.height(Dimensions.spacing_8))
        val expiryTop = paymentUiState.expiryMillis
        if (expiryTop != null) {
            val dateStr = remember(expiryTop) {
                val df = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                df.format(Date(expiryTop))
            }
            Text(
                text = stringResource(R.string.premium_expires_txt, dateStr),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
            )
        } else {
            Text(
                text = stringResource(R.string.premium_active_status_txt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
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
    val shouldShowNotificationItem = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        } else {
            // For API < 33, permission is not required, so don't show the item.
            false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimensions.corner_radius_large))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(vertical = Dimensions.spacing_8)
    ) {
        if (shouldShowNotificationItem) {
            SettingsItem(
                icon = Icons.Outlined.Notifications,
                title = stringResource(R.string.settings_notifications_txt),
                onClick = onNotificationNavAction
            )
        }
        SettingsItem(
            icon = Icons.Outlined.Language,
            title = stringResource(R.string.settings_language_txt),
            onClick = onLanguageNavAction
        )
    }
}

@Composable
fun LegalSection() {
    val context = LocalContext.current
    val onPrivacyPolicyClick = remember(context) { { context.openUrlInBrowser("https://data.androidplay.in/wfy/privacy-policy") } }
    val onTermsOfUseClick = remember(context) { { context.openUrlInBrowser("https://data.androidplay.in/wfy/terms-and-conditions") } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimensions.corner_radius_large))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(vertical = Dimensions.spacing_8)
    ) {
        SettingsItem(
            icon = Icons.Outlined.PrivacyTip,
            title = stringResource(R.string.legal_privacy_policy_txt),
            onClick = onPrivacyPolicyClick
        )
        SettingsItem(
            icon = Icons.Outlined.Gavel,
            title = stringResource(R.string.legal_terms_of_use_txt),
            onClick = onTermsOfUseClick
        )
        SettingsItem(
            icon = Icons.Outlined.Info,
            title = stringResource(R.string.legal_app_version_txt),
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
            .padding(horizontal = Dimensions.spacing_16, vertical = Dimensions.spacing_16),
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
                modifier = Modifier.size(Dimensions.icon_default),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.width(Dimensions.spacing_16))
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
            Spacer(modifier = Modifier.width(Dimensions.spacing_12))
            trailingContent()
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.arrow_right_icon_content),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
