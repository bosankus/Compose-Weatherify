package bose.ankush.weatherify.presentation.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import bose.ankush.weatherify.R
import bose.ankush.weatherify.presentation.theme.icons.myiconpack.MyIconPack
import bose.ankush.weatherify.presentation.theme.icons.myiconpack.myiconpack.Account
import bose.ankush.weatherify.presentation.theme.icons.myiconpack.myiconpack.Home
import bose.ankush.weatherify.presentation.theme.icons.myiconpack.myiconpack.Radar


@Composable
fun AppBottomNavigation(
    navController: NavController,
) {
    val selectedItem = remember { mutableStateOf(0) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navController.currentDestination
    val screenItems = listOf(
        Screen.HomeScreen,
        Screen.AirQualityDetailsScreen,
        Screen.LanguageScreen
    )
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, bottom = 16.dp)
            .clip(RoundedCornerShape(10.dp))
            .height(60.dp)
    ) {
        screenItems.forEachIndexed { index, screen ->
            NavigationBarItem(
                icon = {
                    when(screen.resourceId) {
                        R.string.home_screen -> Icon(
                            imageVector = MyIconPack.Home,
                            contentDescription = stringResource(id = screen.resourceId)
                        )
                        R.string.aq_screen -> Icon(
                            imageVector = MyIconPack.Radar,
                            contentDescription = stringResource(id = screen.resourceId)
                        )
                        R.string.account_screen -> Icon(
                            imageVector = MyIconPack.Account,
                            contentDescription = stringResource(id = screen.resourceId)
                        )
                    }
                },
                selected = selectedItem.value == index,
                onClick = {
                    selectedItem.value = index
                    /*navController.navigate(item.route) {
                        popUpTo(
                            id = navController.graph.startDestinationId,
                            popUpToBuilder = { saveState = true }
                        )
                        launchSingleTop = true
                        restoreState = true
                    }*/
                }
            )
        }
    }
}