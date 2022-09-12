package bose.ankush.weatherify.presentation.cities.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
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
import bose.ankush.weatherify.presentation.ui.theme.TextWhite

@Composable
fun CityListItem(
    cityNameList: List<CityName>,
    position: Int,
    onItemClick: (Int, String) -> Unit
) {
    var selectedItem: Int? by remember { mutableStateOf(null) }
    val cityName = cityNameList[position].name ?: DEFAULT_CITY_NAME
    Text(
        text = cityName,
        style = MaterialTheme.typography.h4,
        color = TextWhite,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 13.dp, end = 16.dp)
            .clickable {
                selectedItem = position
                onItemClick(position, cityName)
            }
            .background(if (selectedItem != position) Color.Transparent else AccentColor)
            .padding(start = 3.dp, top = 10.dp, bottom = 10.dp)
    )

}