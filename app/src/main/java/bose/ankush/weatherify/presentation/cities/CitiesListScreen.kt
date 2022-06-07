package bose.ankush.weatherify.presentation.cities

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import bose.ankush.weatherify.R
import bose.ankush.weatherify.common.ConnectivityManager.isNetworkAvailable
import bose.ankush.weatherify.domain.model.CityName
import bose.ankush.weatherify.navigation.Screen
import bose.ankush.weatherify.presentation.cities.component.CityListItem
import bose.ankush.weatherify.presentation.cities.state.CityNameState
import bose.ankush.weatherify.presentation.home.component.ShowError
import bose.ankush.weatherify.presentation.home.component.ShowLoading
import bose.ankush.weatherify.presentation.ui.theme.BackgroundGrey
import bose.ankush.weatherify.presentation.ui.theme.TextWhite

@Composable
fun CitiesListScreen(
    navController: NavController,
    viewModel: CitiesViewModel = hiltViewModel()
) {
    ShowLoading(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGrey)
    )

    val context: Context = LocalContext.current
    val state: CityNameState = viewModel.cityNameState.value

    // Has internet
    if (isNetworkAvailable(context)) {
        // Screen Loading state
        if (state.isLoading) ShowLoading(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGrey)
        )
        // Screen Error state
        else if (!state.error?.asString(context).isNullOrEmpty()) ShowError(
            error = state.error?.asString(context),
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGrey)
        )
        else ShowUIContainer(
            state = state,
            navController = navController
        )
    } else ShowError(
        error = stringResource(id = R.string.no_internet_txt),
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGrey)
    )
}


@Composable
private fun ShowUIContainer(
    state: CityNameState,
    navController: NavController,
) {
    Box(
        modifier = Modifier
            .background(BackgroundGrey)
            .fillMaxSize()
    ) {
        LazyColumn {
            item { CityNameHeader(navController) }

            if (state.names.isNotEmpty()) {
                items(state.names.size) {
                    CityListItem(cityNameList = state.names, position = it) {
                        navController.navigate(Screen.HomeScreen.route)
                    }
                }
            }
        }
    }
}


@Composable
private fun CityNameHeader(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(all = 16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_close),
                tint = Color.White,
                contentDescription = stringResource(id = R.string.close_icon_content),
                modifier = Modifier.clickable {
                    navController.navigate(Screen.HomeScreen.route)
                }
            )
            Text(
                modifier = Modifier.padding(start = 25.dp),
                text = stringResource(id = R.string.select_city),
                style = MaterialTheme.typography.h4,
                color = TextWhite
            )
        }
    }
}
