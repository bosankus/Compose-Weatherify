package bose.ankush.sunriseui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * A custom glassmorphic Snackbar component for displaying error messages.
 *
 * @param message The error message to display
 * @param isVisible Whether the Snackbar is visible
 * @param onDismiss Callback when the Snackbar is dismissed
 * @param durationMillis Duration in milliseconds before the Snackbar is automatically dismissed
 * @param modifier Modifier for the Snackbar
 */
@Composable
fun GlassmorphicSnackbar(
    message: String,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    durationMillis: Long = 3000,
    modifier: Modifier = Modifier
) {
    // Auto-dismiss after durationMillis
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(durationMillis)
            onDismiss()
        }
    }

    // Animation for the Snackbar
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                slideInVertically(
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    initialOffsetY = { it }
                ),
        exit = fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                slideOutVertically(
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    targetOffsetY = { it }
                )
    ) {
        // Glassmorphic Snackbar
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(8.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onError
            )
        }
    }
}

/**
 * A composable that manages the state of a GlassmorphicSnackbar.
 *
 * @param modifier Modifier for the Snackbar
 * @return A pair of (showSnackbar: (String) -> Unit, SnackbarContent: @Composable () -> Unit)
 */
@Composable
fun rememberGlassmorphicSnackbarState(
    modifier: Modifier = Modifier
): Pair<(String) -> Unit, @Composable () -> Unit> {
    var isVisible by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    val showSnackbar: (String) -> Unit = { newMessage ->
        message = newMessage
        isVisible = true
    }

    val snackbarContent: @Composable () -> Unit = {
        GlassmorphicSnackbar(
            message = message,
            isVisible = isVisible,
            onDismiss = { isVisible = false },
            modifier = modifier
        )
    }

    return Pair(showSnackbar, snackbarContent)
}