package bose.ankush.home.presentation.wander

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bose.ankush.commonui.theme.ToastOnWarning
import bose.ankush.commonui.theme.WarningYellow
import bose.ankush.finder.domain.model.LocationSuggestion
import bose.ankush.home.generated.resources.Res
import bose.ankush.home.generated.resources.wander_places_add
import bose.ankush.home.generated.resources.wander_places_add_action
import bose.ankush.home.generated.resources.wander_places_pick_hint
import bose.ankush.home.generated.resources.wander_places_save_error
import bose.ankush.home.generated.resources.wander_places_saved_badge
import bose.ankush.home.generated.resources.wander_places_saving
import bose.ankush.home.generated.resources.wander_places_search_clear
import bose.ankush.home.generated.resources.wander_places_search_close
import bose.ankush.home.generated.resources.wander_places_search_empty
import bose.ankush.home.generated.resources.wander_places_search_error
import bose.ankush.home.generated.resources.wander_places_search_hint
import bose.ankush.home.generated.resources.wander_places_search_label
import bose.ankush.home.generated.resources.wander_places_search_min
import bose.ankush.home.generated.resources.wander_places_show_action
import bose.ankush.home.presentation.places.WanderPlaceSearchState
import bose.ankush.home.presentation.places.WanderPlacesIntent
import bose.ankush.home.presentation.places.WanderPlacesLogic
import org.jetbrains.compose.resources.stringResource

/**
 * Add-a-place sheet on the shared [WanderSheetPanel], the same container as the alert
 * details. It opens fully (no peek) from the bottom, wraps its content and sits on top of
 * the keyboard. Type a city, pick a result, then the primary button saves it and makes it
 * the active location (or just switches to it when it is already saved).
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
    val keyboard = LocalSoftwareKeyboardController.current
    var picked by remember { mutableStateOf<LocationSuggestion?>(null) }
    val selected = WanderAddPlaceAction.retainSelection(picked, search.results)
    val action = WanderAddPlaceAction.of(selected = selected, isSaving = search.isSaving, isSaved = isSaved)
    Column(
        // Shrinks before the field or the button do when the keyboard leaves little room.
        modifier = Modifier.weight(1f, fill = false).fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        SearchField(query = search.query, onQueryChange = { onIntent(WanderPlacesIntent.QueryChanged(it)) })
        SearchProgress(visible = search.isSearching || search.isSaving)
        searchStatus(search, hasPick = selected != null)?.let { status ->
            Text(text = status, color = WanderSheetMuted, fontSize = 13.sp)
        }
        if (search.results.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false).fillMaxWidth().height(ResultsHeight),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(search.results) { suggestion ->
                    SuggestionRow(
                        suggestion = suggestion,
                        flags = SuggestionFlags(saved = isSaved(suggestion), selected = suggestion == selected),
                        enabled = !search.isSaving,
                        onClick = {
                            keyboard?.hide()
                            picked = suggestion
                        },
                    )
                }
            }
        }
    }
    AddPlaceButton(
        action = action,
        onClick = { selected?.let { onIntent(WanderPlacesIntent.SaveSuggestion(it)) } },
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 18.dp),
    )
}

@Composable
private fun SearchProgress(visible: Boolean) {
    // Keeps its 2dp slot so results do not jump when a search starts or ends.
    Box(modifier = Modifier.fillMaxWidth().height(2.dp)) {
        if (visible) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = WarningYellow,
                trackColor = Color.Transparent,
            )
        }
    }
}

@Composable
private fun searchStatus(
    search: WanderPlaceSearchState,
    hasPick: Boolean,
): String? {
    val searchable = WanderPlacesLogic.isSearchable(search.query)
    return when {
        search.saveFailed -> stringResource(Res.string.wander_places_save_error)
        search.searchFailed -> stringResource(Res.string.wander_places_search_error)
        search.query.isNotBlank() && !searchable -> stringResource(Res.string.wander_places_search_min)
        searchable && !search.isSearching && search.results.isEmpty() ->
            stringResource(Res.string.wander_places_search_empty)
        search.results.isNotEmpty() && !hasPick -> stringResource(Res.string.wander_places_pick_hint)
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
        label = { Text(text = stringResource(Res.string.wander_places_search_label)) },
        placeholder = { Text(text = stringResource(Res.string.wander_places_search_hint)) },
        leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
        trailingIcon = { ClearButton(visible = query.isNotEmpty(), onClear = { onQueryChange("") }) },
        keyboardOptions =
            KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        shape = FieldShape,
        colors = wanderFieldColors(),
    )
}

@Composable
private fun ClearButton(
    visible: Boolean,
    onClear: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(CLEAR_ANIM_MILLIS)) + scaleIn(tween(CLEAR_ANIM_MILLIS)),
        exit = fadeOut(tween(CLEAR_ANIM_MILLIS)) + scaleOut(tween(CLEAR_ANIM_MILLIS)),
    ) {
        IconButton(onClick = onClear) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(Res.string.wander_places_search_clear),
            )
        }
    }
}

@Composable
private fun wanderFieldColors() =
    TextFieldDefaults.colors(
        focusedContainerColor = FieldFill,
        unfocusedContainerColor = FieldFill,
        focusedTextColor = WanderOnDark,
        unfocusedTextColor = WanderOnDark,
        focusedLabelColor = WarningYellow,
        unfocusedLabelColor = WanderSheetMuted,
        focusedPlaceholderColor = WanderSheetMuted,
        unfocusedPlaceholderColor = WanderSheetMuted,
        focusedLeadingIconColor = WanderOnDark,
        unfocusedLeadingIconColor = WanderSheetMuted,
        focusedTrailingIconColor = WanderSheetMuted,
        unfocusedTrailingIconColor = WanderSheetMuted,
        cursorColor = WarningYellow,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
    )

private data class SuggestionFlags(
    val saved: Boolean,
    val selected: Boolean,
)

@Composable
private fun SuggestionRow(
    suggestion: LocationSuggestion,
    flags: SuggestionFlags,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val detail = WanderPlacesLogic.suggestionDetail(suggestion)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RowShape)
                .background(if (flags.selected) SelectedRowFill else Color.Transparent)
                .then(if (flags.selected) Modifier.border(1.dp, SelectedRowOutline, RowShape) else Modifier)
                .semantics { selected = flags.selected }
                .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            tint = if (flags.selected) WarningYellow else WanderSheetMuted,
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
        SuggestionEnd(flags = flags)
    }
}

@Composable
private fun SuggestionEnd(flags: SuggestionFlags) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (flags.saved) {
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
        AnimatedVisibility(
            visible = flags.selected,
            enter = fadeIn(tween(PICK_ANIM_MILLIS)) + scaleIn(tween(PICK_ANIM_MILLIS), initialScale = PICK_START_SCALE),
            exit = fadeOut(tween(PICK_ANIM_MILLIS)) + scaleOut(tween(PICK_ANIM_MILLIS), targetScale = PICK_START_SCALE),
        ) {
            Box(
                modifier = Modifier.size(24.dp).clip(CircleShape).background(WarningYellow),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = ToastOnWarning,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun AddPlaceButton(
    action: WanderAddPlaceAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = action.enabled,
        shape = FieldShape,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = WarningYellow,
                contentColor = ToastOnWarning,
                disabledContainerColor = FieldFill,
                disabledContentColor = WanderSheetMuted,
            ),
        modifier = modifier.height(ButtonHeight),
    ) {
        if (action == WanderAddPlaceAction.Saving) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = WanderSheetMuted,
                strokeWidth = 2.dp,
            )
            Spacer(modifier = Modifier.size(10.dp))
        }
        Text(text = addPlaceLabel(action), fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun addPlaceLabel(action: WanderAddPlaceAction): String =
    when (action) {
        WanderAddPlaceAction.ShowSaved -> stringResource(Res.string.wander_places_show_action)
        WanderAddPlaceAction.Saving -> stringResource(Res.string.wander_places_saving)
        WanderAddPlaceAction.PickFirst, WanderAddPlaceAction.Add -> stringResource(Res.string.wander_places_add_action)
    }

private val FieldFill = WanderOnDark.copy(alpha = 0.10f)
private val SelectedRowFill = WarningYellow.copy(alpha = 0.10f)
private val SelectedRowOutline = WarningYellow.copy(alpha = 0.6f)
private val FieldShape = RoundedCornerShape(16.dp)
private val RowShape = RoundedCornerShape(14.dp)
private val ResultsHeight = 264.dp
private val ButtonHeight = 52.dp
private const val CLEAR_ANIM_MILLIS = 150
private const val PICK_ANIM_MILLIS = 200
private const val PICK_START_SCALE = 0.6f
