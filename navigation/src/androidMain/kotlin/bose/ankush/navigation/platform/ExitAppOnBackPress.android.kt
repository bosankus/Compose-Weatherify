package bose.ankush.navigation.platform

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun ExitAppOnBackPress(enabled: Boolean) {
    val activity = LocalActivity.current
    BackHandler(enabled = enabled) { activity?.finish() }
}

@Composable
actual fun rememberExitAppAction(): () -> Unit {
    val activity = LocalActivity.current
    return remember(activity) { { activity?.finish() } }
}
