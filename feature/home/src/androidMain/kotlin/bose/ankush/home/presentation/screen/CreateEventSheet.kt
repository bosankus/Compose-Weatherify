package bose.ankush.home.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bose.ankush.home.generated.resources.Res
import bose.ankush.home.generated.resources.event_date_hint
import bose.ankush.home.generated.resources.event_save_action
import bose.ankush.home.generated.resources.event_saving
import bose.ankush.home.generated.resources.event_sheet_close
import bose.ankush.home.generated.resources.event_sheet_no_place
import bose.ankush.home.generated.resources.event_sheet_subtitle
import bose.ankush.home.generated.resources.event_sheet_title
import bose.ankush.home.generated.resources.event_time_hint
import bose.ankush.home.generated.resources.event_title_hint
import bose.ankush.home.presentation.nearby.NearbyIntent
import bose.ankush.home.presentation.nearby.NearbyState
import org.jetbrains.compose.resources.stringResource

/**
 * New-event sheet, on the same [SheetPanel] and chrome as the add-a-place sheet: title row,
 * a subtitle naming the place the event is pinned to, the fields, and one primary button.
 * It sits on top of the keyboard and keeps the draft when it is closed and reopened.
 */
@Composable
internal fun CreateEventSheet(
    event: CreateEventBinding,
    modifier: Modifier = Modifier,
) {
    val state = event.state
    val keyboard = LocalSoftwareKeyboardController.current
    val close = {
        keyboard?.hide()
        event.onIntent(NearbyIntent.DismissComposer)
    }
    val closeLabel = stringResource(Res.string.event_sheet_close)
    SheetPanel(
        visible = state.composer.isVisible,
        spec = SheetPanelSpec(onDismiss = close, closeLabel = closeLabel, peekFraction = null),
        modifier = modifier.aboveKeyboard(),
        header = {
            SheetTitleBar(
                title = stringResource(Res.string.event_sheet_title),
                closeLabel = closeLabel,
                onClose = close,
            )
        },
        body = {
            EventForm(state = state, onIntent = event.onIntent)
            SheetPrimaryButton(
                text =
                    stringResource(
                        if (state.composer.isSubmitting) Res.string.event_saving else Res.string.event_save_action,
                    ),
                onClick = {
                    keyboard?.hide()
                    event.onIntent(NearbyIntent.SubmitEvent)
                },
                enabled = state.canSubmitEvent,
                busy = state.composer.isSubmitting,
                modifier = Modifier.padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 16.dp,
                    bottom = 18.dp
                ),
            )
        },
    )
}

@Composable
private fun EventForm(
    state: NearbyState,
    onIntent: (NearbyIntent) -> Unit,
) {
    val draft = state.composer.draft
    val placeName = state.placeName?.takeIf { it.isNotBlank() }
    val titleFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { titleFocus.requestFocus() }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(modifier = Modifier.height(2.dp))
        SheetSubtitle(
            text =
                if (placeName != null) {
                    stringResource(Res.string.event_sheet_subtitle, placeName)
                } else {
                    stringResource(Res.string.event_sheet_no_place)
                },
        )
        SheetField(
            value = draft.title,
            onValueChange = { onIntent(NearbyIntent.TitleChanged(it)) },
            spec =
                SheetFieldSpec(
                    placeholder = stringResource(Res.string.event_title_hint),
                    leadingIcon = Icons.Outlined.EditNote,
                    keyboardOptions =
                        KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next
                        ),
                ),
            modifier = Modifier.focusRequester(titleFocus),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SheetField(
                value = draft.dateText,
                onValueChange = { onIntent(NearbyIntent.DateChanged(it)) },
                spec =
                    SheetFieldSpec(
                        placeholder = stringResource(Res.string.event_date_hint),
                        leadingIcon = Icons.Outlined.CalendarMonth,
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next,
                            ),
                    ),
                modifier = Modifier.weight(DATE_WEIGHT),
            )
            SheetField(
                value = draft.timeText,
                onValueChange = { onIntent(NearbyIntent.TimeChanged(it)) },
                spec =
                    SheetFieldSpec(
                        placeholder = stringResource(Res.string.event_time_hint),
                        leadingIcon = Icons.Outlined.Schedule,
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done,
                            ),
                    ),
                modifier = Modifier.weight(1f),
            )
        }
        val error = state.composer.error
        AnimatedVisibility(
            visible = error != null,
            enter = expandVertically(tween(ERROR_ANIM_MILLIS)) + fadeIn(tween(ERROR_ANIM_MILLIS)),
            exit = shrinkVertically(tween(ERROR_ANIM_MILLIS)) + fadeOut(tween(ERROR_ANIM_MILLIS)),
        ) {
            Text(text = error.orEmpty(), color = SheetMutedText, fontSize = 13.sp)
        }
    }
}

private const val DATE_WEIGHT = 1.25f
private const val ERROR_ANIM_MILLIS = 200
