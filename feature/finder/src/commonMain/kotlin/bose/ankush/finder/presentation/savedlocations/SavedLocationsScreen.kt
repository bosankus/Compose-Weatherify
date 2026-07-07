package bose.ankush.finder.presentation.savedlocations

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import bose.ankush.commonui.components.ShimmerEffect
import bose.ankush.finder.domain.model.Location
import bose.ankush.finder.domain.model.LocationSuggestion
import bose.ankush.finder.generated.resources.Res
import bose.ankush.finder.generated.resources.add_icon_content
import bose.ankush.finder.generated.resources.cancel_btn_txt
import bose.ankush.finder.generated.resources.delete_icon_content
import bose.ankush.finder.generated.resources.saved_locations_empty_txt
import bose.ankush.finder.generated.resources.saved_locations_premium_desc
import bose.ankush.finder.generated.resources.saved_locations_premium_title
import bose.ankush.finder.generated.resources.saved_locations_premium_upgrade_btn_txt
import bose.ankush.finder.generated.resources.saved_locations_title
import bose.ankush.finder.generated.resources.set_as_default_confirm_btn
import bose.ankush.finder.generated.resources.set_as_default_dialog_body
import bose.ankush.finder.generated.resources.set_as_default_dialog_title
import bose.ankush.finder.generated.resources.set_as_default_dialog_warning
import bose.ankush.finder.presentation.placesearch.PlaceSearchDialog
import org.jetbrains.compose.resources.stringResource
import kotlin.math.round

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SavedLocationsScreen(
    state: SavedLocationsState,
    onIntent: (SavedLocationsIntent) -> Unit,
    bottomBar: @Composable () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val pendingLocation = remember { mutableStateOf<Location?>(null) }

    pendingLocation.value?.let { location ->
        SetAsDefaultLocationDialog(
            locationName = location.name,
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
                        text = stringResource(Res.string.saved_locations_title),
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
                !state.isPremium -> PremiumGate()
                state.isLoading && state.locations.isEmpty() -> ShowLoading()
                state.locations.isEmpty() -> EmptyLocations()
                else ->
                    LocationList(
                        locations = state.locations,
                        onDelete = { onIntent(SavedLocationsIntent.Delete(it)) },
                        onLocationClick = { pendingLocation.value = it },
                    )
            }
        }
    }
}

@Composable
private fun PremiumGate() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.saved_locations_premium_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(Res.string.saved_locations_premium_desc),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { /* TODO: Handle upgrade to premium */ }) {
            Text(text = stringResource(Res.string.saved_locations_premium_upgrade_btn_txt))
        }
    }
}

@Composable
private fun ShowLoading(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(5) {
            LocationCardSkeleton()
        }
    }
}

@Composable
private fun EmptyLocations() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.saved_locations_empty_txt),
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
            )
        }
    }
}

@Composable
private fun LocationCardSkeleton() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                ShimmerEffect(
                    height = 20.dp,
                    cornerRadius = 4.dp,
                    modifier = Modifier.fillMaxWidth(0.8f),
                )
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerEffect(
                    height = 12.dp,
                    cornerRadius = 4.dp,
                    modifier = Modifier.fillMaxWidth(0.4f),
                )
            }
            ShimmerEffect(
                modifier = Modifier.padding(8.dp).size(20.dp),
                height = 20.dp,
                cornerRadius = 10.dp,
            )
        }
    }
}

@Composable
private fun LocationCard(
    location: Location,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
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
                modifier = Modifier.weight(1f),
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
                    contentDescription = stringResource(Res.string.delete_icon_content),
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
private fun AddLocationFab(onPlaceSelected: (LocationSuggestion) -> Unit) {
    val showDialog = remember { mutableStateOf(false) }

    FloatingActionButton(onClick = { showDialog.value = true }) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(Res.string.add_icon_content),
        )
    }

    if (showDialog.value) {
        PlaceSearchDialog(
            onDismiss = { showDialog.value = false },
            onPlaceSelected = { place ->
                showDialog.value = false
                onPlaceSelected(place)
            },
        )
    }
}

@Composable
private fun SetAsDefaultLocationDialog(
    locationName: String,
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
                text = stringResource(Res.string.set_as_default_dialog_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(Res.string.set_as_default_dialog_body, locationName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider()
                Text(
                    text = stringResource(Res.string.set_as_default_dialog_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(Res.string.set_as_default_confirm_btn))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel_btn_txt))
            }
        },
    )
}

private val previewLocations =
    listOf(
        Location(id = "1", name = "San Francisco, USA", lat = 37.7749, lon = -122.4194),
        Location(id = "2", name = "Kolkata, India", lat = 22.5726, lon = 88.3639),
        Location(id = "3", name = "Tokyo, Japan", lat = 35.6762, lon = 139.6503),
    )

@Preview
@Composable
private fun SetAsDefaultLocationDialogPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            SetAsDefaultLocationDialog(
                locationName = previewLocations.first().name,
                onConfirm = {},
                onDismiss = {},
            )
        }
    }
}

@Preview
@Composable
private fun SavedLocationsScreenPremiumGatePreview() {
    SavedLocationsScreen(
        state = SavedLocationsState(isPremium = false),
        onIntent = {},
    )
}

@Preview
@Composable
private fun SavedLocationsScreenEmptyPreview() {
    SavedLocationsScreen(
        state = SavedLocationsState(isPremium = true, isLoading = false, locations = emptyList()),
        onIntent = {},
    )
}

@Preview
@Composable
private fun SavedLocationsScreenLoadingPreview() {
    SavedLocationsScreen(
        state = SavedLocationsState(isPremium = true, isLoading = true),
        onIntent = {},
    )
}

@Preview
@Composable
private fun SavedLocationsScreenListPreview() {
    SavedLocationsScreen(
        state = SavedLocationsState(isPremium = true, locations = previewLocations),
        onIntent = {},
    )
}

@Preview
@Composable
private fun LocationCardSkeletonPreview() {
    MaterialTheme {
        LocationCardSkeleton()
    }
}

@Preview
@Composable
private fun LocationCardPreview() {
    MaterialTheme {
        LocationCard(
            location = previewLocations.first(),
            onClick = {},
            onDelete = {},
        )
    }
}
