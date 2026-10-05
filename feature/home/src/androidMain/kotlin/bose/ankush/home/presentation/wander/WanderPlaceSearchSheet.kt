package bose.ankush.home.presentation.wander

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bose.ankush.commonui.theme.WarningYellow
import bose.ankush.finder.domain.model.LocationSuggestion
import bose.ankush.home.generated.resources.Res
import bose.ankush.home.generated.resources.wander_places_add
import bose.ankush.home.generated.resources.wander_places_save_error
import bose.ankush.home.generated.resources.wander_places_saved_badge
import bose.ankush.home.generated.resources.wander_places_search_clear
import bose.ankush.home.generated.resources.wander_places_search_close
import bose.ankush.home.generated.resources.wander_places_search_empty
import bose.ankush.home.generated.resources.wander_places_search_error
import bose.ankush.home.generated.resources.wander_places_search_hint
import bose.ankush.home.generated.resources.wander_places_search_min
import bose.ankush.home.presentation.places.WanderPlaceSearchState
import bose.ankush.home.presentation.places.WanderPlacesIntent
import bose.ankush.home.presentation.places.WanderPlacesLogic
import org.jetbrains.compose.resources.stringResource

/**
 * Add-a-place sheet on the shared [WanderSheetPanel], the same container as the alert
 * details. It opens fully (no peek) at a fixed height so results never resize it, and
 * rises with the keyboard. Tapping a result saves it and makes it the active location.
 */
@Composable
internal fun WanderPlaceSearchSheet(
    places: WanderPlacesBinding,
    modifier: Modifier = Modifier,
) {
    val search = places.state.search
    val keyboard = LocalSoftwareKeyboardController.current
    val close = {
        keyboard?.hide()
        places.onIntent(WanderPlacesIntent.CloseSearch)
    }
    WanderSheetPanel(
        visible = search.isOpen,
        spec =
            WanderSheetSpec(
                onDismiss = close,
                closeLabel = stringResource(Res.string.wander_places_search_close),
                peekFraction = null,
                heightFraction = SHEET_HEIGHT_FRACTION,
            ),
        modifier = modifier.aboveKeyboard(),
        header = { SearchHeader(onClose = close) },
        body = {
            SearchBody(
                search = search,
                isSaved = { WanderPlacesLogic.findSaved(it, places.state.places) != null },
                onIntent = { intent ->
                    if (intent is WanderPlacesIntent.SaveSuggestion) keyboard?.hide()
                    places.onIntent(intent)
                },
            )
        },
    )
}

/**
 * Lifts the sheet over the keyboard. The home column already pads the navigation bar and
 * leaves [TabBarReserveHeight] for the tab bar under this area, so both count as consumed and
 * only the part of the keyboard above them pads the sheet. Applied in layout, so it tracks
 * the keyboard animation frame by frame. Needs `adjustResize` on the activity: without it
 * Android also resizes or pans the window, and the keyboard is counted twice.
 */
private fun Modifier.aboveKeyboard(): Modifier =
    consumeWindowInsets(PaddingValues(bottom = TabBarReserveHeight)).imePadding()

@Composable
private fun SearchHeader(onClose: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(Res.string.wander_places_add),
            color = WanderOnDark,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(Res.string.wander_places_search_close),
                tint = WanderOnDark,
            )
        }
    }
}

@Composable
private fun ColumnScope.SearchBody(
    search: WanderPlaceSearchState,
    isSaved: (LocationSuggestion) -> Boolean,
    onIntent: (WanderPlacesIntent) -> Unit,
) {
    Column(
        modifier = Modifier.weight(1f).fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SearchField(query = search.query, onQueryChange = { onIntent(WanderPlacesIntent.QueryChanged(it)) })
        if (search.isSearching || search.isSaving) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = WarningYellow,
                trackColor = Color.Transparent,
            )
        }
        searchStatus(search)?.let { status ->
            Text(text = status, color = WanderSheetMuted, fontSize = 13.sp)
        }
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(search.results) { suggestion ->
                SuggestionRow(
                    suggestion = suggestion,
                    saved = isSaved(suggestion),
                    enabled = !search.isSaving,
                    onClick = { onIntent(WanderPlacesIntent.SaveSuggestion(suggestion)) },
                )
            }
        }
    }
}

@Composable
private fun searchStatus(search: WanderPlaceSearchState): String? {
    val searchable = WanderPlacesLogic.isSearchable(search.query)
    return when {
        search.saveFailed -> stringResource(Res.string.wander_places_save_error)
        search.searchFailed -> stringResource(Res.string.wander_places_search_error)
        search.query.isNotBlank() && !searchable -> stringResource(Res.string.wander_places_search_min)
        searchable && !search.isSearching && search.results.isEmpty() ->
            stringResource(Res.string.wander_places_search_empty)
        else -> null
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
        singleLine = true,
        placeholder = { Text(text = stringResource(Res.string.wander_places_search_hint), color = WanderSheetMuted) },
        leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = WanderSheetMuted) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(Res.string.wander_places_search_clear),
                        tint = WanderSheetMuted,
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        shape = RoundedCornerShape(14.dp),
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = FieldFill,
                unfocusedContainerColor = FieldFill,
                focusedTextColor = WanderOnDark,
                unfocusedTextColor = WanderOnDark,
                cursorColor = WarningYellow,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
    )
}

@Composable
private fun SuggestionRow(
    suggestion: LocationSuggestion,
    saved: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val detail = WanderPlacesLogic.suggestionDetail(suggestion)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            tint = WanderSheetMuted,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                text = WanderPlacesLogic.savedName(suggestion),
                color = WanderOnDark,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (detail.isNotBlank()) {
                Text(
                    text = detail,
                    color = WanderSheetMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (saved) {
            Text(
                text = stringResource(Res.string.wander_places_saved_badge),
                color = WanderOnDark,
                fontSize = 12.sp,
                modifier =
                    Modifier
                        .background(FieldFill, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

private val FieldFill = WanderOnDark.copy(alpha = 0.10f)
private const val SHEET_HEIGHT_FRACTION = 0.9f
