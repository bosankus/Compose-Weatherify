package bose.ankush.weatherify.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import bose.ankush.weatherify.R

@Composable
fun AppBottomBar(
    isVisible: MutableState<Boolean>,
    navController: NavController
) {
    val selectedItem = remember { mutableIntStateOf(0) }
    val screenItems = listOf(
        Screen.HomeNestedNav,
        Screen.RunNestedNav,
        Screen.ProfileNestedNav
    )

    AnimatedVisibility(
        visible = isVisible.value,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
    ) {
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .height(80.dp)
        ) {
            screenItems.forEachIndexed { index, screen ->
                NavigationBarItem(
                    icon = {
                        when (screen.resourceId) {
                            R.string.home_nested_nav -> Icon(
                                painter = painterResource(id = R.drawable.ic_home),
                                contentDescription = stringResource(id = screen.resourceId)
                            )

                            R.string.run_nested_nav -> Icon(
                                painter = painterResource(id = R.drawable.ic_run),
                                contentDescription = stringResource(id = screen.resourceId)
                            )

                            R.string.profile_nested_nav -> Icon(
                                painter = painterResource(id = R.drawable.ic_profile),
                                contentDescription = stringResource(id = screen.resourceId)
                            )
                        }
                    },
                    selected = selectedItem.intValue == index,
                    onClick = {
                        selectedItem.intValue = index
                        navController.navigate(screen.route)
                    }
                )
            }
        }
    }
}