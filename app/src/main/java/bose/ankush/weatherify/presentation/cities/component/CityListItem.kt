package bose.ankush.weatherify.presentation.cities.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import bose.ankush.weatherify.common.DEFAULT_CITY_NAME
import bose.ankush.weatherify.domain.model.CityName
import bose.ankush.weatherify.presentation.ui.theme.AccentColor

@Composable
fun CityListItem(
    cityNameList: List<CityName>,
    position: Int,
    onItemClick: (Int, String) -> Unit
) {
    var selectedItem by remember { mutableStateOf(0) }
    val cityName = cityNameList[position].name ?: DEFAULT_CITY_NAME
    Text(
        text = cityName,
        style = MaterialTheme.typography.h3,
        modifier = Modifier
            .padding(all = 16.dp)
            .clickable {
                selectedItem = position
                onItemClick(position, cityName)
            },
        color = if (selectedItem == position) AccentColor else Color.White
    )

}