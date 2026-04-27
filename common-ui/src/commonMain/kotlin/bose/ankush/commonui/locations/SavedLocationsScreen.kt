package bose.ankush.commonui.locations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import bose.ankush.network.model.PlaceSuggestion
import bose.ankush.network.model.SavedLocation

// ============ UI State Classes ============

/**
 * State for the saved locations feature.
 */
data class SavedLocationsUiState(
    val isPremium: Boolean = false,
    val isLoading: Boolean = false,
    val locations: List<SavedLocation> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null
)

/**
 * State for the place search feature.
 */
data class PlaceSearchUiState(
    val searchQuery: String = "",
    val results: List<PlaceSuggestion> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// ============ Strings ============

/**
 * Localized strings for SavedLocationsScreen.
 * All with English defaults for KMP compatibility.
 */
data class SavedLocationsStrings(
    val title: String = "Saved Locations",
    val premiumTitle: String = "Premium Feature",
    val premiumDesc: String = "Save your favorite locations to access them quickly. Upgrade to premium to unlock this feature.",
    val emptyText: String = "No saved locations yet. Add one to get started!",
    val searchHint: String = "Search for a place",
    val searchDialogTitle: String = "Add Location",
    val noResults: String = "No results found for \"%s\"",
    val deleteContentDesc: String = "Delete location",
    val addContentDesc: String = "Add location",
    val cancelBtn: String = "Cancel",
    val saveSuccessMsg: String = "Location saved successfully",
    val deleteSuccessMsg: String = "Location deleted successfully"
)

// ============ Main Screen ============

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedLocationsScreen(
    locationsState: SavedLocationsUiState,
    searchState: PlaceSearchUiState,
    onQueryChanged: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSaveLocation: (name: String, lat: Double, lon: Double) -> Unit,
    onDeleteLocation: (String) -> Unit,
    onMessageShown: () -> Unit,
    strings: SavedLocationsStrings = SavedLocationsStrings(),
    bottomBar: @Composable () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar on success or error
    LaunchedEffect(locationsState.successMessage, locationsState.error) {
        val message = locationsState.successMessage ?: locationsState.error
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = strings.title,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (locationsState.isPremium) {
                AddLocationFab(
                    onPlaceSelected = { place ->
                        onSaveLocation(
                            place.name,
                            place.latitude.toDouble(),
                            place.longitude.toDouble()
                        )
                    },
                    searchState = searchState,
                    onQueryChanged = onQueryChanged,
                    onClearSearch = onClearSearch,
                    strings = strings
                )
            }
        },
        bottomBar = bottomBar
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                !locationsState.isPremium -> PremiumGate(strings)
                locationsState.isLoading && locationsState.locations.isEmpty() -> ShowLoading()
                locationsState.locations.isEmpty() -> EmptyLocations(strings)
                else -> LocationList(
                    locations = locationsState.locations,
                    onDelete = onDeleteLocation,
                    strings = strings
                )
            }
        }
    }
}

// ============ Composable Components ============

@Composable
private fun PremiumGate(strings: SavedLocationsStrings) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = strings.premiumTitle,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = strings.premiumDesc,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ShowLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyLocations(strings: SavedLocationsStrings) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = strings.emptyText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp)
        )
    }
}

@Composable
private fun LocationList(
    locations: List<SavedLocation>,
    onDelete: (String) -> Unit,
    strings: SavedLocationsStrings
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 16.dp,
            vertical = 12.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(locations, key = { it.id }) { location ->
            LocationCard(
                location = location,
                onDelete = { onDelete(location.id) },
                strings = strings
            )
        }
    }
}

@Composable
private fun LocationCard(
    location: SavedLocation,
    onDelete: () -> Unit,
    strings: SavedLocationsStrings
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatCoordinates(location.lat, location.lon),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = strings.deleteContentDesc,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun formatCoordinates(lat: Double, lon: Double): String {
    val latFormatted = (lat * 10000).toInt() / 10000.0
    val lonFormatted = (lon * 10000).toInt() / 10000.0
    return "$latFormatted, $lonFormatted"
}

@Composable
private fun AddLocationFab(
    onPlaceSelected: (PlaceSuggestion) -> Unit,
    searchState: PlaceSearchUiState,
    onQueryChanged: (String) -> Unit,
    onClearSearch: () -> Unit,
    strings: SavedLocationsStrings
) {
    val showDialog = remember { mutableStateOf(false) }

    FloatingActionButton(onClick = { showDialog.value = true }) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = strings.addContentDesc
        )
    }

    if (showDialog.value) {
        PlaceSearchDialog(
            onDismiss = { showDialog.value = false },
            onPlaceSelected = { place ->
                showDialog.value = false
                onPlaceSelected(place)
            },
            searchState = searchState,
            onQueryChanged = onQueryChanged,
            onClearSearch = onClearSearch,
            strings = strings
        )
    }
}

@Composable
private fun PlaceSearchDialog(
    onDismiss: () -> Unit,
    onPlaceSelected: (PlaceSuggestion) -> Unit,
    searchState: PlaceSearchUiState,
    onQueryChanged: (String) -> Unit,
    onClearSearch: () -> Unit,
    strings: SavedLocationsStrings
) {
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = {
            onClearSearch()
            onDismiss()
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.searchDialogTitle)
                IconButton(
                    onClick = {
                        onClearSearch()
                        onDismiss()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = strings.cancelBtn,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = searchState.searchQuery,
                    onValueChange = onQueryChanged,
                    placeholder = { Text(strings.searchHint) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )

                AnimatedVisibility(
                    visible = searchState.isLoading,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }

                AnimatedVisibility(
                    visible = searchState.error != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (searchState.error != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.errorContainer,
                                    shape = MaterialTheme.shapes.small
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = searchState.error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = searchState.searchQuery.length >= 2 && searchState.results.isEmpty() && !searchState.isLoading && searchState.error == null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.noResults.replace("%s", searchState.searchQuery),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                AnimatedVisibility(
                    visible = searchState.results.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            searchState.results,
                            key = { "${it.name}_${it.latitude}_${it.longitude}" }
                        ) { place ->
                            PlaceSuggestionItem(
                                place = place,
                                onClick = {
                                    onClearSearch()
                                    onPlaceSelected(place)
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = {
                onClearSearch()
                onDismiss()
            }) {
                Text(strings.cancelBtn)
            }
        }
    )

    // Request focus on dialog appearance
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun PlaceSuggestionItem(
    place: PlaceSuggestion,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small
            )
            .padding(vertical = 12.dp, horizontal = 12.dp)
    ) {
        Text(
            text = place.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = listOfNotNull(place.city, place.state, place.country)
                .filter { it.isNotEmpty() }
                .joinToString(", "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
