package bose.ankush.weatherify.presentation.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import bose.ankush.weatherify.R
import bose.ankush.weatherify.domain.model.AvgForecast
import bose.ankush.weatherify.presentation.home.HomeViewModel

@Composable
fun FourDaysForecastLayout(
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val fourDaysForecasts = viewModel.getFourDaysAvgForecast()
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.forecast_heading_txt),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp)
        )
        FutureForecastListItem(avgForecastList = fourDaysForecasts) {
            val selectedDate = fourDaysForecasts[it].date
            selectedDate?.let { date -> viewModel.getDayWiseDetailedForecast(date) }
        }
    }
}


@Composable
fun FutureForecastListItem(
    avgForecastList: List<AvgForecast>,
    onItemClick: (Int) -> Unit
) {
    var selectedItem by remember { mutableStateOf(0) }
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 16.dp)
    ) {
        items(avgForecastList.size) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        selectedItem = it
                        onItemClick(it)
                    }
                    .background(if (selectedItem == it) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.inversePrimary)
                    .padding(horizontal = 10.dp, vertical = 20.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = avgForecastList[it].nameOfDay?.substring(0,3) ?: stringResource(id = R.string.not_available),
                        style = MaterialTheme.typography.bodySmall,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.alpha(0.6f),
                    )
                    Text(
                        text = stringResource(
                            id = R.string.celsius,
                            avgForecastList[it].avgTemp ?: stringResource(id = R.string.not_available)
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(
                        text = stringResource(
                            id = R.string.feels_like,
                            avgForecastList[it].feelsLike ?: stringResource(id = R.string.not_available)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .alpha(0.6f),
                    )
                }
            }
        }
    }
}
