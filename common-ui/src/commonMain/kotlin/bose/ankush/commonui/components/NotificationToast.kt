package bose.ankush.commonui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

enum class ToastType { SUCCESS, WARNING, ERROR }

/**
 * Holds the measured height of an anchor component (e.g. bottom nav bar) so
 * [NotificationToast] can automatically position itself above it.
 */
@Stable
class ToastAnchorState internal constructor(private val density: Float) {
    var anchorHeight: Dp by mutableStateOf(0.dp)
        internal set

    internal fun updateHeight(heightPx: Int) {
        anchorHeight = (heightPx / density).dp
    }
}

@Composable
fun rememberToastAnchorState(): ToastAnchorState {
    val density = LocalDensity.current
    return remember { ToastAnchorState(density.density) }
}

/**
 * Attach to the component above which the toast should appear (e.g. bottom nav bar).
 * Measures the component's height and reports it to [ToastAnchorState].
 */
fun Modifier.toastAnchor(state: ToastAnchorState): Modifier =
    this.onSizeChanged { size -> state.updateHeight(size.height) }

/**
 * Multiplatform toast notification overlay.
 *
 * Displays an animated toast message at the bottom of the screen, auto-dismissing after
 * [durationMillis]. Position can be adjusted via [anchorState] (measures a bottom component's
 * height) or a manual [bottomOffset].
 *
 * Supports Android and iOS via Compose Multiplatform.
 */
@Composable
fun NotificationToast(
    modifier: Modifier = Modifier,
    message: String,
    title: String,
    type: ToastType,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    durationMillis: Long = 3000,
    bottomOffset: Dp = 0.dp,
    anchorState: ToastAnchorState? = null,
) {
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(durationMillis)
            onDismiss()
        }
    }

    val (backgroundColor, icon, iconColor) = when (type) {
        ToastType.SUCCESS -> Triple(Color(0xFFE5F3E5), Icons.Filled.CheckCircle, Color(0xFF3F8F3F))
        ToastType.WARNING -> Triple(Color(0xFFFFF4E5), Icons.Filled.Warning, Color(0xFFFFA500))
        ToastType.ERROR -> Triple(Color(0xFFFDE5E5), Icons.Filled.Close, Color(0xFFB00020))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp + (anchorState?.anchorHeight ?: bottomOffset)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                slideInVertically(
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    initialOffsetY = { it },
                ),
            exit = fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                slideOutVertically(
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    targetOffsetY = { it },
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(backgroundColor)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
