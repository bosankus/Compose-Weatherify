package bose.ankush.weatherify.presentation.strings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import bose.ankush.finder.presentation.savedlocations.SavedLocationsStrings
import bose.ankush.weatherify.R

@Composable
fun rememberSavedLocationsStrings(): SavedLocationsStrings {
    val noResultsTemplate = stringResource(R.string.place_search_no_results)
    val setAsDefaultBodyTemplate = stringResource(R.string.set_as_default_dialog_body)
    return SavedLocationsStrings(
        title = stringResource(R.string.saved_locations_title),
        premiumTitle = stringResource(R.string.saved_locations_premium_title),
        premiumDesc = stringResource(R.string.saved_locations_premium_desc),
        emptyText = stringResource(R.string.saved_locations_empty_txt),
        searchHint = stringResource(R.string.place_search_hint),
        searchDialogTitle = stringResource(R.string.place_search_dialog_title),
        noResults = { query -> noResultsTemplate.replace("%1\$s", query) },
        deleteContentDesc = stringResource(R.string.delete_icon_content),
        addContentDesc = stringResource(R.string.add_icon_content),
        cancelBtn = stringResource(R.string.cancel_btn_txt),
        saveSuccessMsg = stringResource(R.string.saved_locations_save_success),
        deleteSuccessMsg = stringResource(R.string.saved_locations_delete_success),
        setAsDefaultDialogTitle = stringResource(R.string.set_as_default_dialog_title),
        setAsDefaultDialogBody = { name -> setAsDefaultBodyTemplate.replace("%1\$s", name) },
        setAsDefaultDialogWarning = stringResource(R.string.set_as_default_dialog_warning),
        setAsDefaultConfirmBtn = stringResource(R.string.set_as_default_confirm_btn),
    )
}
