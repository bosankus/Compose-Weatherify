package bose.ankush.navigation.platform

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable

@Composable
actual fun ExitAppOnBackPress(enabled: Boolean) {
    val activity = LocalActivity.current
    BackHandler(enabled = enabled) { activity?.finish() }
}
