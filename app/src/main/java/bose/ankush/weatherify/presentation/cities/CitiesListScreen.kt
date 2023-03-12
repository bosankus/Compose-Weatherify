package bose.ankush.weatherify.presentation.cities

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import bose.ankush.weatherify.R
import bose.ankush.weatherify.navigation.Screen
import bose.ankush.weatherify.presentation.cities.component.CityListItem
import bose.ankush.weatherify.presentation.home.state.ShowLoading
import bose.ankush.weatherify.presentation.ui.theme.DefaultCardBackgroundLightGrey
import bose.ankush.weatherify.presentation.ui.theme.SeaGreenDark
import bose.ankush.weatherify.presentation.ui.theme.TextWhite

@Composable
fun CitiesListScreen(
    navController: NavController,
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column {
            CityNameHeader(navController)

            CityNameSearchBarWithList(navController)
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

@Composable
private fun CityNameSearchBarWithList(navController: NavController) {
    val viewModels: CitiesViewModel = hiltViewModel()
    val searchText by viewModels.searchText.collectAsState()
    val isSearching by viewModels.isSearching.collectAsState()
    val cityName by viewModels.cityName.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
    ) {
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp)),
            value = searchText,
            onValueChange = viewModels::onSearchTextChange,
            placeholder = { Text(text = stringResource(id = R.string.select_city) + "...") },
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = DefaultCardBackgroundLightGrey,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = TextWhite,
                textColor = TextWhite,
                placeholderColor = SeaGreenDark
            )
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (isSearching) {
            ShowLoading(modifier = Modifier.fillMaxSize())
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(cityName.size) {
                    CityListItem(cityNameList = cityName, position = it) { _, name ->
                        navController.navigate(Screen.HomeScreen.withArgs(name))
                    }
                }
            }
        }
    }
}
