package bose.ankush.home.ios

import bose.ankush.finder.domain.model.Location
import bose.ankush.finder.domain.model.LocationSuggestion
import bose.ankush.home.generated.resources.Res
import bose.ankush.home.generated.resources.event_save_action
import bose.ankush.home.generated.resources.event_saving
import bose.ankush.home.generated.resources.event_sheet_no_place
import bose.ankush.home.generated.resources.event_sheet_subtitle
import bose.ankush.home.generated.resources.saved_places_add_action
import bose.ankush.home.generated.resources.saved_places_delete_desc
import bose.ankush.home.generated.resources.saved_places_delete_error
import bose.ankush.home.generated.resources.saved_places_pick_hint
import bose.ankush.home.generated.resources.saved_places_save_error
import bose.ankush.home.generated.resources.saved_places_saving
import bose.ankush.home.generated.resources.saved_places_search_empty
import bose.ankush.home.generated.resources.saved_places_search_error
import bose.ankush.home.generated.resources.saved_places_search_min
import bose.ankush.home.generated.resources.saved_places_select_error
import bose.ankush.home.generated.resources.saved_places_show_action
import bose.ankush.home.presentation.nearby.NearbyState
import bose.ankush.home.presentation.nearby.toDayMonthLabel
import bose.ankush.home.presentation.places.SavedPlaceSearchState
import bose.ankush.home.presentation.places.SavedPlacesLogic
import bose.ankush.home.presentation.places.SavedPlacesNotice
import bose.ankush.home.presentation.places.SavedPlacesState
import bose.ankush.home.presentation.screen.AddPlaceAction
import bose.ankush.home.presentation.util.SectionState
import bose.ankush.home.presentation.util.valueOrNull
import org.jetbrains.compose.resources.getString

internal suspend fun SavedPlacesState.toPlacesPageUi(onPlacesPage: Boolean): PlacesPageUi =
    PlacesPageUi(
        isPremium = isPremium,
        isLoading = isLoading,
        loadFailed = loadFailed,
        notice =
            when (notice) {
                SavedPlacesNotice.DeleteFailed -> getString(Res.string.saved_places_delete_error)
                SavedPlacesNotice.SelectFailed -> getString(Res.string.saved_places_select_error)
                null -> null
            },
        currentIsActive = active == null,
        places =
            places.map { place ->
                PlaceUi(
                    key = SavedPlacesLogic.placeKey(place),
                    title = place.name,
                    detail = SavedPlacesLogic.formatCoordinates(place.lat, place.lon),
                    isActive = SavedPlacesLogic.isActive(place, active),
                    deleteLabel =
                        place.id
                            .takeIf { it.isNotBlank() }
                            ?.let { getString(Res.string.saved_places_delete_desc, place.name) },
                )
            },
        showAddButton =
            AddPlaceAction.showAddButton(
                onPlacesPage = onPlacesPage,
                isPremium = isPremium,
                sheetOpen = search.isOpen,
            ),
    )

/** Same button and status rules as the Android sheet, from [AddPlaceAction] and the search state. */
internal suspend fun SavedPlacesState.toPlaceSearchUi(picked: String?): PlaceSearchUi {
    val results = search.results.distinct()
    val selected =
        AddPlaceAction.retainSelection(results.firstOrNull { it.key() == picked }, results)
    val isSaved =
        { suggestion: LocationSuggestion -> SavedPlacesLogic.findSaved(suggestion, places) != null }
    val action =
        AddPlaceAction.of(selected = selected, isSaving = search.isSaving, isSaved = isSaved)
    return PlaceSearchUi(
        isOpen = search.isOpen,
        query = search.query,
        results =
            results.map { suggestion ->
                SuggestionUi(
                    key = suggestion.key(),
                    name = SavedPlacesLogic.savedName(suggestion),
                    detail = SavedPlacesLogic.suggestionDetail(suggestion),
                    isSaved = isSaved(suggestion),
                    isSelected = suggestion == selected,
                )
            },
        isSearching = search.isSearching,
        isSaving = search.isSaving,
        status = search.status(hasPick = selected != null),
        actionLabel =
            when (action) {
                AddPlaceAction.ShowSaved -> getString(Res.string.saved_places_show_action)
                AddPlaceAction.Saving -> getString(Res.string.saved_places_saving)
                AddPlaceAction.PickFirst, AddPlaceAction.Add -> getString(Res.string.saved_places_add_action)
            },
        actionEnabled = action.enabled,
    )
}

private suspend fun SavedPlaceSearchState.status(hasPick: Boolean): String? {
    val searchable = SavedPlacesLogic.isSearchable(query)
    return when {
        saveFailed -> getString(Res.string.saved_places_save_error)
        searchFailed -> getString(Res.string.saved_places_search_error)
        query.isNotBlank() && !searchable -> getString(Res.string.saved_places_search_min)
        searchable && !isSearching && results.isEmpty() -> getString(Res.string.saved_places_search_empty)
        results.isNotEmpty() && !hasPick -> getString(Res.string.saved_places_pick_hint)
        else -> null
    }
}

/** Stable key for a suggestion row; also how Swift names a pick back to the controller. */
internal fun LocationSuggestion.key(): String = "$name|$city|$state|$country|$latitude|$longitude"

internal fun List<Location>.findByKey(key: String): Location? =
    firstOrNull { SavedPlacesLogic.placeKey(it) == key }

internal suspend fun NearbyState.toComposerUi(): EventComposerUi {
    val place = placeName?.takeIf { it.isNotBlank() }
    return EventComposerUi(
        isVisible = composer.isVisible,
        subtitle =
            if (place != null) {
                getString(Res.string.event_sheet_subtitle, place)
            } else {
                getString(Res.string.event_sheet_no_place)
            },
        title = composer.draft.title,
        dateText = composer.draft.dateText,
        timeText = composer.draft.timeText,
        error = composer.error,
        isSubmitting = composer.isSubmitting,
        canSubmit = canSubmitEvent,
        actionLabel = getString(if (composer.isSubmitting) Res.string.event_saving else Res.string.event_save_action),
    )
}

/** Events and the closest saved place; null when neither has anything to show. */
internal fun NearbyState.toNearbyUi(): NearbyUi? {
    val loaded = events.valueOrNull().orEmpty()
    val eventsLoading = events is SectionState.Loading
    val eventsFailed = events is SectionState.Failed
    val saved =
        featuredPlace.valueOrNull()?.let { place ->
            FeaturedPlaceUi(
                name = place.name.trim(),
                subtitle = listOf(place.city, place.state, place.country).filter { it.isNotBlank() }
                    .joinToString(", "),
            )
        }
    // Same rule as Android: the block shows once there are events or a named saved place.
    if (loaded.isEmpty() && saved?.name?.isNotBlank() != true) return null
    return NearbyUi(
        showEvents = loaded.isNotEmpty() || eventsFailed,
        eventsLoading = eventsLoading,
        eventsFailed = eventsFailed,
        events =
            loaded.mapIndexed { index, event ->
                NearbyEventUi(
                    key = "$index-${event.date}",
                    title = event.title,
                    whenLabel = event.date.toDayMonthLabel(),
                )
            },
        savedPlace = saved,
    )
}
