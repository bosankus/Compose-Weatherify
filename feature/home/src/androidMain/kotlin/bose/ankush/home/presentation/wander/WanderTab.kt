package bose.ankush.home.presentation.wander

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.ui.graphics.vector.ImageVector

enum class WanderTab(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Home", Icons.Filled.Home),
    WEATHER("Weather", Icons.Filled.WbCloudy),
    MAP("Map", Icons.Filled.Map),
    TRAVEL("Travel", Icons.Filled.Flight),
    HUB("Hub", Icons.Filled.GridView),
}
