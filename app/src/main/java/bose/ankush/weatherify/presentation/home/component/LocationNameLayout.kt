package bose.ankush.weatherify.presentation.home.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import bose.ankush.weatherify.R
import bose.ankush.weatherify.navigation.Screen
import bose.ankush.weatherify.presentation.home.HomeViewModel
import bose.ankush.weatherify.presentation.ui.theme.TextWhite

@Composable
fun LocationNameLayout(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val cityName = viewModel.cityName.value ?: stringResource(id = R.string.not_available)
    Row(
        modifier = Modifier
            .padding(all = 16.dp)
            .fillMaxWidth()
            .clickable { navController.navigate(Screen.CitiesListScreen.route) },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_location),
            tint = Color.White,
            contentDescription = stringResource(id = R.string.location_icon_content),
        )
        Text(
            modifier = Modifier.padding(start = 5.dp),
            text = cityName,
            style = MaterialTheme.typography.h4,
            color = TextWhite,
        )
    }
}