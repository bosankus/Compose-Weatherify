package bose.ankush.finder.presentation.savedlocations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import bose.ankush.finder.domain.model.LocationSuggestion
import bose.ankush.finder.presentation.placesearch.PlaceSearchIntent
import bose.ankush.finder.presentation.placesearch.PlaceSearchState
import bose.ankush.finder.presentation.placesearch.PlaceSearchViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Owns a private [ViewModelStore] scoped to this dialog's presence in composition —
 * [PlaceSearchViewModel] is created when the dialog opens and cleared on dismiss,
 * independent of the host screen's (longer-lived) ViewModelStore.
 */
@Composable
internal fun PlaceSearchDialog(
    onDismiss: () -> Unit,
    onPlaceSelected: (LocationSuggestion) -> Unit,
    strings: SavedLocationsStrings,
) {
    val dialogVmOwner =
        remember {
            object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            }
        }
    DisposableEffect(Unit) { onDispose { dialogVmOwner.viewModelStore.clear() } }

    CompositionLocalProvider(LocalViewModelStoreOwner provides dialogVmOwner) {
        val viewModel = koinViewModel<PlaceSearchViewModel>()
        val state by viewModel.state.collectAsState()

        PlaceSearchDialogContent(
            state = state,
            onQueryChanged = { viewModel.processIntent(PlaceSearchIntent.QueryChanged(it)) },
            onDismiss = {
                viewModel.processIntent(PlaceSearchIntent.Clear)
                onDismiss()
            },
            onPlaceSelected = { place ->
                viewModel.processIntent(PlaceSearchIntent.Clear)
                onPlaceSelected(place)
            },
            strings = strings,
        )
    }
}

@Composable
private fun PlaceSearchDialogContent(
    state: PlaceSearchState,
    onQueryChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onPlaceSelected: (LocationSuggestion) -> Unit,
    strings: SavedLocationsStrings,
) {
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.searchDialogTitle) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onQueryChanged,
                    placeholder = { Text(strings.searchHint) },
                    singleLine = true,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                )

                AnimatedVisibility(
                    visible = state.isLoading,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }

                AnimatedVisibility(
                    visible = state.error != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    if (state.error != null) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.errorContainer,
                                        shape = MaterialTheme.shapes.small,
                                    ).padding(12.dp),
                        ) {
                            Text(
                                text = state.error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible =
                        state.searchQuery.length >= 2 &&
                            state.results.isEmpty() &&
                            !state.isLoading &&
                            state.error == null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = strings.noResults(state.searchQuery),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                AnimatedVisibility(
                    visible = state.results.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(
                            state.results,
                            key = { "${it.name}_${it.latitude}_${it.longitude}" },
                        ) { place ->
                            PlaceSuggestionItem(
                                place = place,
                                onClick = { onPlaceSelected(place) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancelBtn)
            }
        },
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun PlaceSuggestionItem(
    place: LocationSuggestion,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.small,
                ).padding(vertical = 12.dp, horizontal = 12.dp),
    ) {
        Text(
            text = place.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text =
                listOfNotNull(place.city, place.state, place.country)
                    .filter { it.isNotEmpty() }
                    .joinToString(", "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
