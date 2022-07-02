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
import bose.ankush.weatherify.presentation.UIState
import bose.ankush.weatherify.presentation.cities.component.CityListItem
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
    val state: UIState<List<CityName>> = viewModel.cityNameState.value

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
            cityList = state.data ?: emptyList(),
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
    cityList: List<CityName>,
    navController: NavController,
) {
    Box(
        modifier = Modifier
            .background(BackgroundGrey)
            .fillMaxSize()
    ) {
        Column {
            CityNameHeader(navController)

            LazyColumn {
                if (cityList.isNotEmpty()) {
                    items(cityList.size) {
                        CityListItem(cityNameList = cityList, position = it) { _, name ->
                            navController.navigate(Screen.HomeScreen.withArgs(name))
                        }
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterEnd)
                .padding(all = 16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.select_city),
                style = MaterialTheme.typography.h3,
                color = TextWhite
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_close),
                tint = TextWhite,
                contentDescription = stringResource(id = R.string.close_icon_content),
                modifier = Modifier
                    .clickable { navController.popBackStack() }

            )
        }
    }
}
