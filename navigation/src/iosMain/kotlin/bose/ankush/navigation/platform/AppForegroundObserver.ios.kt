@file:OptIn(ExperimentalForeignApi::class)

package bose.ankush.navigation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidBecomeActiveNotification

@Composable
actual fun ObserveAppForeground(onForeground: () -> Unit) {
    val currentOnForeground by rememberUpdatedState(onForeground)
    DisposableEffect(Unit) {
        val observer =
            NSNotificationCenter.defaultCenter.addObserverForName(
                name = UIApplicationDidBecomeActiveNotification,
                `object` = null,
                queue = NSOperationQueue.mainQueue,
            ) { _ ->
                currentOnForeground()
            }
        onDispose {
            NSNotificationCenter.defaultCenter.removeObserver(observer)
        }
    }
}
