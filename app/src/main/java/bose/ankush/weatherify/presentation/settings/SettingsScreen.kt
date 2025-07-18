package bose.ankush.weatherify.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import bose.ankush.weatherify.R
import bose.ankush.weatherify.base.LocaleConfigMapper
import bose.ankush.weatherify.presentation.MainViewModel
import bose.ankush.weatherify.presentation.navigation.AppBottomBar
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    viewModel: MainViewModel,
    navController: NavController,
    onLanguageNavAction: (Array<String>) -> Unit,
    onNotificationNavAction: () -> Unit
) {
    val isNotificationBannerVisible = viewModel.showNotificationCardItem.collectAsState().value
    rememberCoroutineScope()
    val languageList = LocaleConfigMapper.getAvailableLanguagesFromJson(
        jsonFile = "countryConfig.json",
        context = LocalContext.current
    )

    // State for Premium bottom sheet
    val showPremiumBottomSheet = remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState()

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
                // Notification block
                if (isNotificationBannerVisible) {
                    // Create a transition state for the animation
                    val transitionState = remember { MutableTransitionState(false) }

                    // Start the animation when the component is first displayed
                    LaunchedEffect(Unit) {
                        delay(100) // Small delay for better visual effect
                        transitionState.targetState = true
                    }

                    AnimatedVisibility(
                        visibleState = transitionState,
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
                                    imageVector = Icons.Filled.KeyboardArrowRight,
                                    contentDescription = "Navigate to language selection",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }

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
                            .clickable { showPremiumBottomSheet.value = true },
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
                                            .background(Color(0xFFFFB74D))
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = "Get Premium",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    modifier = Modifier.padding(start = 24.dp),
                                    text = "Upgrade to Premium and unlock exclusive features, priority support, and an ad-free experience.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFFB74D).copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowRight,
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
                                onDismiss = { showPremiumBottomSheet.value = false }
                            )
                        }
                    }
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
private fun PremiumBottomSheetContent(
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Simplified Header
        Text(
            text = "Premium",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Condensed Features List
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                SimplePremiumFeature("Ad-Free Experience")
                SimplePremiumFeature("Extended 15-day Forecasts")
                SimplePremiumFeature("Severe Weather Alerts")
                SimplePremiumFeature("Detailed Air Quality Data")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Simplified Pricing
        Text(
            text = "$4.99/month",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "7-day free trial, cancel anytime",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Subscribe Button
        Button(
            onClick = { onDismiss() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFB74D)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Subscribe",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Cancel Button
        Text(
            text = "No Thanks",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable { onDismiss() }
                .padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun SimplePremiumFeature(
    feature: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFB74D))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = feature,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenHeader(modifier: Modifier = Modifier) {

    // Create a transition state for the animation
    val headerTransitionState = remember { MutableTransitionState(false) }

    // Start the animation when the component is first displayed
    LaunchedEffect(Unit) {
        headerTransitionState.targetState = true
    }

    AnimatedVisibility(
        visibleState = headerTransitionState,
        enter = fadeIn(animationSpec = tween(durationMillis = 500)) +
                slideInVertically(
                    animationSpec = tween(durationMillis = 500),
                    initialOffsetY = { -it / 2 }
                ),
        exit = fadeOut()
    ) {
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
