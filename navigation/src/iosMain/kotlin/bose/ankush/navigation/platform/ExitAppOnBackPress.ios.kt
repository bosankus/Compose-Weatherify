package bose.ankush.navigation.platform

import androidx.compose.runtime.Composable

// iOS has no hardware/gesture back button that exits an app — there is nothing to handle here.
@Composable
actual fun ExitAppOnBackPress(enabled: Boolean) = Unit

// Apple's HIG disallows apps from programmatically quitting themselves.
@Composable
actual fun rememberExitAppAction(): () -> Unit = {}
