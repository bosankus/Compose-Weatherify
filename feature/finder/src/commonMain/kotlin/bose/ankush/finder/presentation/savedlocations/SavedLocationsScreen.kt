package bose.ankush.finder.presentation.savedlocations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import bose.ankush.finder.domain.model.Location
import bose.ankush.finder.domain.model.LocationSuggestion
import kotlin.math.round

@Immutable
data class SavedLocationsStrings(
    val title: String = "Saved Locations",
    val premiumTitle: String = "Premium Feature",
    val premiumDesc: String =
        "Save your favorite locations to access them quickly. " +
            "Upgrade to premium to unlock this feature.",
    val emptyText: String = "No saved locations yet. Add one to get started!",
    val searchHint: String = "Search for a place",
    val searchDialogTitle: String = "Add Location",
    val noResults: (String) -> String = { "No results found for \"$it\"" },
    val deleteContentDesc: String = "Delete location",
    val addContentDesc: String = "Add location",
    val cancelBtn: String = "Cancel",
    val saveSuccessMsg: String = "Location saved successfully",
    val deleteSuccessMsg: String = "Location deleted successfully",
    val setAsDefaultDialogTitle: String = "Use as weather location?",
    val setAsDefaultDialogBody: (
        String,
    ) -> String = { "Weather data will show for $it instead of your current GPS position." },
    val setAsDefaultDialogWarning: String = "Your live GPS location won't update while this is active.",
    val setAsDefaultConfirmBtn: String = "Set as Default",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SavedLocationsScreen(
    state: SavedLocationsState,
    onIntent: (SavedLocationsIntent) -> Unit,
    strings: SavedLocationsStrings = SavedLocationsStrings(),
    bottomBar: @Composable () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val pendingLocation = remember { mutableStateOf<Location?>(null) }

    pendingLocation.value?.let { location ->
        SetAsDefaultLocationDialog(
            locationName = location.name,
            strings = strings,
            onConfirm = {
                onIntent(SavedLocationsIntent.SelectLocation(location))
                pendingLocation.value = null
            },
            onDismiss = { pendingLocation.value = null },
        )
    }

    LaunchedEffect(state.successMessage, state.error) {
        val message = state.successMessage ?: state.error
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onIntent(SavedLocationsIntent.MessageShown)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = strings.title,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (state.isPremium) {
                AddLocationFab(
                    onPlaceSelected = { place ->
                        onIntent(
                            SavedLocationsIntent.Save(
                                place.name,
                                place.latitude,
                                place.longitude,
                            ),
                        )
                    },
                    strings = strings,
                )
            }
        },
        bottomBar = bottomBar,
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            when {
                !state.isPremium -> PremiumGate(strings)
                state.isLoading && state.locations.isEmpty() -> ShowLoading()
                state.locations.isEmpty() -> EmptyLocations(strings)
                else ->
                    LocationList(
                        locations = state.locations,
                        onDelete = { onIntent(SavedLocationsIntent.Delete(it)) },
                        onLocationClick = { pendingLocation.value = it },
                        strings = strings,
                    )
            }
        }
    }
}

@Composable
private fun PremiumGate(strings: SavedLocationsStrings) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = strings.premiumTitle,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = strings.premiumDesc,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ShowLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyLocations(strings: SavedLocationsStrings) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = strings.emptyText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp),
        )
    }
}

@Composable
private fun LocationList(
    locations: List<Location>,
    onDelete: (String) -> Unit,
    onLocationClick: (Location) -> Unit,
    strings: SavedLocationsStrings,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            locations.distinctBy { it.id.ifEmpty { "${it.lat}_${it.lon}_${it.name}" } },
            key = { it.id.ifEmpty { "${it.lat}_${it.lon}_${it.name}" } },
        ) { location ->
            LocationCard(
                location = location,
                onClick = { onLocationClick(location) },
                onDelete = { onDelete(location.id) },
                strings = strings,
            )
        }
    }
}

@Composable
private fun LocationCard(
    location: Location,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    strings: SavedLocationsStrings,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatCoordinates(location.lat, location.lon),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = strings.deleteContentDesc,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

private fun formatCoordinates(
    lat: Double,
    lon: Double,
): String {
    val latRounded = round(lat * 10000) / 10000.0
    val lonRounded = round(lon * 10000) / 10000.0
    return "$latRounded, $lonRounded"
}

@Composable
private fun AddLocationFab(
    onPlaceSelected: (LocationSuggestion) -> Unit,
    strings: SavedLocationsStrings,
) {
    val showDialog = remember { mutableStateOf(false) }

    FloatingActionButton(onClick = { showDialog.value = true }) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = strings.addContentDesc,
        )
    }

    if (showDialog.value) {
        PlaceSearchDialog(
            onDismiss = { showDialog.value = false },
            onPlaceSelected = { place ->
                showDialog.value = false
                onPlaceSelected(place)
            },
            strings = strings,
        )
    }
}

@Composable
private fun SetAsDefaultLocationDialog(
    locationName: String,
    strings: SavedLocationsStrings,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = strings.setAsDefaultDialogTitle,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = strings.setAsDefaultDialogBody(locationName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                    Text(
                        text = strings.setAsDefaultDialogWarning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(strings.setAsDefaultConfirmBtn)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancelBtn)
            }
        },
    )
}
