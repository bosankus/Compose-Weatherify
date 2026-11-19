package bose.ankush.auth.presentation

import androidx.compose.runtime.Composable

/** System back closes the form sheet. iOS has no back button, so it does nothing there. */
@Composable
internal expect fun LoginBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
)
