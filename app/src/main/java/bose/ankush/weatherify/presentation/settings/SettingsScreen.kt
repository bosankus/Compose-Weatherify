package bose.ankush.weatherify.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import bose.ankush.sunriseui.premium.PremiumBottomSheetContent
import bose.ankush.weatherify.R
import bose.ankush.weatherify.base.LocaleConfigMapper
import bose.ankush.weatherify.presentation.AuthState
import bose.ankush.weatherify.presentation.MainViewModel
import bose.ankush.weatherify.presentation.navigation.AppBottomBar
import bose.ankush.weatherify.presentation.payment.PaymentStage
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("KotlinConstantConditions")
@Composable
internal fun SettingsScreen(
    viewModel: MainViewModel,
    navController: NavController,
    onLanguageNavAction: (Array<String>) -> Unit,
    onNotificationNavAction: () -> Unit
) {
    val isNotificationBannerVisible = viewModel.showNotificationCardItem.collectAsState().value
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            ScreenHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 50.dp)
            )
        },
        content = { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                // Premium Active Top Card (always on top when active)
                val isPremiumActiveTop =
                    paymentUiState.stage == PaymentStage.Success || paymentUiState.isPremiumActivated
                if (isPremiumActiveTop) {
                    val topTransition = remember { MutableTransitionState(false) }
                    LaunchedEffect(Unit) { topTransition.targetState = true }
                    AnimatedVisibility(
                        visibleState = topTransition,
                        enter = fadeIn(animationSpec = tween(600)) +
                                slideInVertically(
                                    animationSpec = tween(600),
                                    initialOffsetY = { -it / 3 }
                                ),
                        exit = fadeOut()
                    ) {
                        val goldBg = Brush.horizontalGradient(
                            listOf(
                                Color(0xFFFFF9C4), // light gold
                                Color(0xFFFFE082),
                                Color(0xFFFFD54F)
                            )
                        )
                        val goldBorder = Brush.horizontalGradient(
                            listOf(Color(0xFFFFF59D), Color(0xFFFFC107))
                        )
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            border = BorderStroke(1.dp, goldBorder)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(goldBg)
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val infinite =
                                                rememberInfiniteTransition(label = "premiumStar")
                                            val starAlpha = infinite.animateFloat(
                                                initialValue = 0.7f,
                                                targetValue = 1f,
                                                animationSpec = infiniteRepeatable(
                                                    animation = tween<Float>(
                                                        durationMillis = 1200,
                                                        easing = LinearEasing
                                                    ),
                                                    repeatMode = RepeatMode.Reverse
                                                ),
                                                label = "starAlpha"
                                            ).value
                                            Icon(
                                                imageVector = Icons.Filled.Star,
                                                contentDescription = "Premium active",
                                                tint = Color(0xFF8D6E63),
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .alpha(starAlpha)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Premium",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF4E342E)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(Color(0xFF2E7D32).copy(alpha = 0.9f))
                                            ) {
                                                Text(
                                                    text = "Active",
                                                    modifier = Modifier.padding(
                                                        horizontal = 10.dp,
                                                        vertical = 4.dp
                                                    ),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            val expiryTop = paymentUiState.expiryMillis
                                            if (expiryTop != null) {
                                                val df = SimpleDateFormat(
                                                    "MMM d, yyyy",
                                                    Locale.getDefault()
                                                )
                                                val dateStr = df.format(Date(expiryTop))
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = "Expires $dateStr",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF5D4037)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // Notification block
                if (isNotificationBannerVisible) {
                    EnterAnimated(delayMillis = 100) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 30.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(all = 20.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.Start
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = "Notification",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    modifier = Modifier.padding(top = 8.dp),
                                    text = "Turn on notification permission to get weather updates on the go.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                )

                                Button(
                                    modifier = Modifier
                                        .padding(top = 16.dp)
                                        .align(Alignment.End),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    onClick = { onNotificationNavAction.invoke() }
                                ) {
                                    Text(
                                        text = "Turn on",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Language block
                // Create a transition state for the animation
                val languageTransitionState = remember { MutableTransitionState(false) }

                // Start the animation when the component is first displayed
                LaunchedEffect(Unit) {
                    delay(200) // Small delay for staggered effect
                    languageTransitionState.targetState = true
                }

                AnimatedVisibility(
                    visibleState = languageTransitionState,
                    enter = fadeIn(animationSpec = tween(durationMillis = 500)) +
                            slideInVertically(
                                animationSpec = tween(durationMillis = 500),
                                initialOffsetY = { it / 2 }
                            ),
                    exit = fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                            .clickable { onLanguageNavAction.invoke(languageList) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(all = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondary)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = "Language",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    modifier = Modifier.padding(start = 24.dp),
                                    text = "Select your preferred language for a personalized experience.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Navigate to language selection",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }

                if (!isPremiumActiveTop) {
                // Get Premium block
                // Create a transition state for the animation
                val premiumTransitionState = remember { MutableTransitionState(false) }

                // Start the animation when the component is first displayed
                LaunchedEffect(Unit) {
                    delay(300) // Small delay for staggered effect
                    premiumTransitionState.targetState = true
                }

                AnimatedVisibility(
                    visibleState = premiumTransitionState,
                    enter = fadeIn(animationSpec = tween(durationMillis = 500)) +
                            slideInVertically(
                                animationSpec = tween(durationMillis = 500),
                                initialOffsetY = { it / 2 }
                            ),
                    exit = fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                            .clickable(
                                enabled = !(
                                        paymentUiState.stage == PaymentStage.CreatingOrder ||
                                                paymentUiState.stage == PaymentStage.AwaitingPayment ||
                                                paymentUiState.stage == PaymentStage.Verifying
                                        ) && !paymentUiState.isPremiumActivated
                            ) { showPremiumBottomSheet.value = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (paymentUiState.isPremiumActivated || paymentUiState.stage == PaymentStage.Success) Color(
                                0xFFFFF3E0
                            ) else MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(all = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFFB74D))
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = if (paymentUiState.isPremiumActivated || paymentUiState.stage == PaymentStage.Success) "Premium" else "Get Premium",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                val isPremiumActive =
                                    paymentUiState.stage == PaymentStage.Success || paymentUiState.isPremiumActivated

                                if (!isPremiumActive) {
                                    Text(
                                        modifier = Modifier.padding(start = 24.dp),
                                        text = "Upgrade to Premium and unlock exclusive features, priority support, and an ad-free experience.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    )
                                }

                                // Activation progress states on Premium card
                                val currentStage = paymentUiState.stage
                                if (currentStage == PaymentStage.CreatingOrder ||
                                    currentStage == PaymentStage.AwaitingPayment ||
                                    currentStage == PaymentStage.Verifying
                                ) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    val target = when (currentStage) {
                                        PaymentStage.CreatingOrder -> 0.33f
                                        PaymentStage.AwaitingPayment -> 0.66f
                                        PaymentStage.Verifying -> 0.9f
                                        else -> 0f
                                    }
                                    val animated =
                                        androidx.compose.animation.core.animateFloatAsState(
                                            targetValue = target,
                                            animationSpec = tween(600),
                                            label = "premiumProgress"
                                        ).value
                                    Text(
                                        modifier = Modifier.padding(start = 24.dp, bottom = 6.dp),
                                        text = when (currentStage) {
                                            PaymentStage.CreatingOrder -> "Creating order..."
                                            PaymentStage.AwaitingPayment -> "Awaiting payment..."
                                            PaymentStage.Verifying -> "Verifying payment..."
                                            else -> ""
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    LinearProgressIndicator(
                                        progress = { animated },
                                        modifier = Modifier
                                            .padding(start = 24.dp, end = 24.dp)
                                            .fillMaxWidth(),
                                        color = Color(0xFFFFB74D)
                                    )
                                }

                                if (paymentUiState.stage == PaymentStage.Success || paymentUiState.isPremiumActivated) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.padding(start = 24.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF2E7D32))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Premium Activated",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    val expiryMs = paymentUiState.expiryMillis
                                    if (expiryMs != null) {
                                        val df =
                                            SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                                        val dateStr = df.format(Date(expiryMs))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            modifier = Modifier.padding(start = 24.dp),
                                            text = "Expires on $dateStr",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                if (paymentUiState.stage == PaymentStage.Failure && (paymentUiState.message?.isNotBlank() == true)) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        modifier = Modifier.padding(start = 24.dp),
                                        text = paymentUiState.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFFB74D).copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Show premium information",
                                    tint = Color(0xFFFFB74D),
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }

                    // Premium Bottom Sheet
                    if (showPremiumBottomSheet.value) {
                        ModalBottomSheet(
                            onDismissRequest = { showPremiumBottomSheet.value = false },
                            sheetState = bottomSheetState,
                            containerColor = MaterialTheme.colorScheme.surface,
                            dragHandle = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(40.dp)
                                            .height(4.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.3f
                                                ),
                                                shape = RoundedCornerShape(2.dp)
                                            )
                                    )
                                }
                            }
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
                }

                }

                // Logout block
                // Create a transition state for the animation
                val logoutTransitionState = remember { MutableTransitionState(false) }
                LaunchedEffect(Unit) {
                    delay(400)
                    logoutTransitionState.targetState = true
                }

                AnimatedVisibility(
                    visibleState = logoutTransitionState,
                    enter = fadeIn(animationSpec = tween(durationMillis = 500)) +
                            slideInVertically(
                                animationSpec = tween(durationMillis = 500),
                                initialOffsetY = { it / 2 }
                            ),
                    exit = fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                            .clickable { showLogoutDialog.value = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(all = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.error)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = "Logout",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    modifier = Modifier.padding(start = 24.dp),
                                    text = "Sign out from this device.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Logout",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }

                if (showLogoutDialog.value) {
                    AlertDialog(
                        onDismissRequest = { if (!isLoggingOut) showLogoutDialog.value = false },
                        title = { Text(text = "Logout") },
                        text = { Text(text = "Are you sure you want to logout?") },
                        confirmButton = {
                            TextButton(onClick = { viewModel.logout() }, enabled = !isLoggingOut) {
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenHeader(modifier: Modifier = Modifier) {

    EnterAnimated(slideFromTop = true) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(id = R.string.settings_screen),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Customize your app experience",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun EnterAnimated(
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    slideFromTop: Boolean = false,
    durationMillis: Int = 500,
    content: @Composable () -> Unit
) {
    val transitionState = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) {
        if (delayMillis > 0) delay(delayMillis.toLong())
        transitionState.targetState = true
    }
    AnimatedVisibility(
        modifier = modifier,
        visibleState = transitionState,
        enter = fadeIn(animationSpec = tween(durationMillis = durationMillis)) +
                slideInVertically(
                    animationSpec = tween(durationMillis = durationMillis),
                    initialOffsetY = { if (slideFromTop) -it / 2 else it / 2 }
                ),
        exit = fadeOut()
    ) { content() }
}
