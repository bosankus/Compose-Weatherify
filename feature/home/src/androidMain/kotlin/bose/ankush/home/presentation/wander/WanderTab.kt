package bose.ankush.home.presentation.wander

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.ui.graphics.vector.ImageVector

enum class WanderTab(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Home", Icons.Filled.Home),
    MAP("Map", Icons.Filled.Map),
    HUB("Hub", Icons.Filled.GridView),
}
