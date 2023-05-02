package bose.ankush.weatherify.presentation.cities

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import bose.ankush.weatherify.presentation.navigation.Screen
import bose.ankush.weatherify.presentation.cities.component.CityListItem
import bose.ankush.weatherify.presentation.home.state.ShowLoading

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
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 13.dp, end = 16.dp, top = 16.dp)
    ) {
        Icon(
            painter = painterResource(id = bose.ankush.language.R.drawable.ic_back),
            tint = MaterialTheme.colorScheme.onBackground,
            contentDescription = stringResource(id = R.string.close_icon_content),
            modifier = Modifier
                .clip(CircleShape)
                .clickable { navController.popBackStack() }
                .padding(all = 3.dp)

        )
        Text(
            text = stringResource(id = R.string.select_city),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.onSecondaryContainer,
                textColor = MaterialTheme.colorScheme.onSecondaryContainer,
                placeholderColor = MaterialTheme.colorScheme.onSecondaryContainer
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
