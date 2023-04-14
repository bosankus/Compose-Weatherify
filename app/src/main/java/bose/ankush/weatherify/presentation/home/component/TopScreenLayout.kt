package bose.ankush.weatherify.presentation.home.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import bose.ankush.weatherify.R
import bose.ankush.weatherify.navigation.Screen
import bose.ankush.weatherify.presentation.home.HomeViewModel
import bose.ankush.weatherify.presentation.ui.theme.Black

@Composable
fun TopScreenLayout(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val cityName = viewModel.cityName.value ?: stringResource(id = R.string.not_available)
    Row(
        modifier = Modifier
            .padding(all = 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        /*Menu icon*/
        Icon(
            modifier = Modifier.width(28.dp).height(28.dp),
            painter = painterResource(id = R.drawable.ic_menu_white),
            tint = Black,
            contentDescription = stringResource(id = R.string.menu_icon_content),
        )
        /*Location name*/
        Text(
            modifier = Modifier.padding(start = 5.dp)
                .clickable { navController.navigate(Screen.CitiesListScreen.route) },
            text = cityName,
            style = MaterialTheme.typography.h4,
            fontSize = 24.sp,
            color = Black,
        )
        /*Language change icon*/
        Icon(
            modifier = Modifier.width(28.dp).height(28.dp)
                .clickable {  },
            painter = painterResource(id = R.drawable.ic_language_white),
            tint = Black,
            contentDescription = stringResource(id = R.string.language_icon_content),
        )
    }
}