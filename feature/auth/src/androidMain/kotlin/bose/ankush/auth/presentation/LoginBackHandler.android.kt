package bose.ankush.auth.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
internal actual fun LoginBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) = BackHandler(enabled = enabled, onBack = onBack)
