package bose.ankush.weatherify.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import bose.ankush.commonui.components.ToastAnchorState
import bose.ankush.commonui.components.toastAnchor
import bose.ankush.weatherify.R

@Composable
fun AppBottomBar(
    isVisible: MutableState<Boolean>,
    navController: NavController,
    toastAnchorState: ToastAnchorState? = null,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val screenItems =
        listOf(
            Screen.HomeNestedNav,
            Screen.SavedLocationsNestedNav,
            Screen.ProfileNestedNav,
        )

    AnimatedVisibility(
        modifier = if (toastAnchorState != null) Modifier.toastAnchor(toastAnchorState) else Modifier,
        visible = isVisible.value,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
    ) {
        // Enhanced Glassmorphic Navigation Bar
        NavigationBar(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .height(64.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    )
                    .background(
                        MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp).copy(alpha = 0.8f),
                    )
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    ),
            containerColor = Color.Transparent, // Make the container transparent to show our custom background
        ) {
            screenItems.forEachIndexed { _, screen ->
                NavigationBarItem(
                    icon = {
                        when (screen.resourceId) {
                            R.string.home_nested_nav ->
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_home),
                                    contentDescription = stringResource(id = screen.resourceId),
                                )

                            R.string.saved_locations_nested_nav ->
                                Icon(
                                    imageVector = Icons.Outlined.BookmarkBorder,
                                    contentDescription = stringResource(id = R.string.saved_locations_icon_content),
                                )

                            R.string.profile_nested_nav ->
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_profile),
                                    contentDescription = stringResource(id = screen.resourceId),
                                )
                        }
                    },
                    selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors =
                        NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        ),
                )
            }
        }
    }
}
