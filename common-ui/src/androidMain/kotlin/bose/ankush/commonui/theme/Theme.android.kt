package bose.ankush.commonui.theme

import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
internal actual fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val context = LocalContext.current
    return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}

@Composable
internal actual fun PlatformSystemBarsEffect(darkTheme: Boolean) {
    val view = LocalView.current
    val activity = LocalActivity.current
    if (view.isInEditMode || activity == null) return

    DisposableEffect(darkTheme) {
        val window = activity.window
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !darkTheme
        controller.isAppearanceLightNavigationBars = !darkTheme
        controller.hide(WindowInsetsCompat.Type.navigationBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        onDispose { }
    }
}

/**
 * Counted per window rather than saved and restored per call: when one dark screen opens
 * another (profile, then language), the outgoing one's cleanup runs after the incoming one has
 * set light icons, and a plain restore would put dark icons back on a dark screen.
 */
@Composable
actual fun LightSystemBarIcons() {
    val view = LocalView.current
    val activity = LocalActivity.current
    if (view.isInEditMode || activity == null) return
    DisposableEffect(activity, view) {
        val controller = WindowCompat.getInsetsController(activity.window, view)
        val request = LightIconRequests.acquire(activity, controller.isAppearanceLightStatusBars)
        controller.isAppearanceLightStatusBars = false
        onDispose {
            LightIconRequests
                .release(request)
                ?.let { original -> controller.isAppearanceLightStatusBars = original }
        }
    }
}

/** Open light-icon requests per activity, and the icon style from before the first one. */
private object LightIconRequests {
    private class Window(
        val originalLight: Boolean,
    ) {
        var count = 0
    }

    private val windows = java.util.WeakHashMap<android.app.Activity, Window>()

    fun acquire(
        activity: android.app.Activity,
        currentLight: Boolean,
    ): android.app.Activity {
        windows.getOrPut(activity) { Window(currentLight) }.count++
        return activity
    }

    /** The style to restore once the last request is gone, else null. */
    fun release(activity: android.app.Activity): Boolean? {
        val window = windows[activity]?.apply { count-- }
        return if (window != null && window.count <= 0) {
            windows.remove(activity)
            window.originalLight
        } else {
            null
        }
    }
}
