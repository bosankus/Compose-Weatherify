package bose.ankush.home.presentation.screen

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bose.ankush.home.generated.resources.Res
import bose.ankush.home.generated.resources.event_date_pick
import bose.ankush.home.generated.resources.event_note_hint
import bose.ankush.home.generated.resources.event_picker_cancel
import bose.ankush.home.generated.resources.event_picker_confirm
import bose.ankush.home.generated.resources.event_save_action
import bose.ankush.home.generated.resources.event_saving
import bose.ankush.home.generated.resources.event_sheet_close
import bose.ankush.home.generated.resources.event_sheet_no_place
import bose.ankush.home.generated.resources.event_sheet_subtitle
import bose.ankush.home.generated.resources.event_sheet_title
import bose.ankush.home.generated.resources.event_time_pick
import bose.ankush.home.generated.resources.event_title_hint
import bose.ankush.home.presentation.nearby.NearbyIntent
import bose.ankush.home.presentation.nearby.NearbyState
import bose.ankush.home.presentation.nearby.toDayMonthLabel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

/**
 * New-event sheet, on the same [SheetPanel] and chrome as the add-a-place sheet: title row,
 * a subtitle naming the place the event is pinned to, the fields, and one primary button.
 * Date and time come from Material3 pickers, not typed text; the note is optional. A tapped
 * day arrives already filled in. It sits on top of the keyboard and keeps the draft when it
 * is closed and reopened.
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
                modifier =
                    Modifier.padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = 16.dp,
                        bottom = 18.dp,
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
    var picker by remember { mutableStateOf(EventPicker.NONE) }
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
                            imeAction = ImeAction.Next,
                        ),
                ),
            modifier = Modifier.focusRequester(titleFocus),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SheetPickerField(
                text = draft.date()?.toDayMonthLabel().orEmpty(),
                placeholder = stringResource(Res.string.event_date_pick),
                icon = Icons.Outlined.CalendarMonth,
                onClick = { picker = EventPicker.DATE },
                modifier = Modifier.weight(DATE_WEIGHT),
            )
            SheetPickerField(
                text = draft.time()?.toHourMinuteLabel().orEmpty(),
                placeholder = stringResource(Res.string.event_time_pick),
                icon = Icons.Outlined.Schedule,
                onClick = { picker = EventPicker.TIME },
                modifier = Modifier.weight(1f),
            )
        }
        SheetField(
            value = draft.note,
            onValueChange = { onIntent(NearbyIntent.NoteChanged(it)) },
            spec =
                SheetFieldSpec(
                    placeholder = stringResource(Res.string.event_note_hint),
                    leadingIcon = Icons.AutoMirrored.Outlined.Notes,
                    keyboardOptions =
                        KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done,
                        ),
                ),
        )
        val error = state.composer.error
        AnimatedVisibility(
            visible = error != null,
            enter = expandVertically(tween(ERROR_ANIM_MILLIS)) + fadeIn(tween(ERROR_ANIM_MILLIS)),
            exit = shrinkVertically(tween(ERROR_ANIM_MILLIS)) + fadeOut(tween(ERROR_ANIM_MILLIS)),
        ) {
            Text(text = error.orEmpty(), color = SheetMutedText, fontSize = 13.sp)
        }
    }
    val closePicker = { picker = EventPicker.NONE }
    when (picker) {
        EventPicker.DATE ->
            EventDatePickerDialog(
                initial = draft.date(),
                onPicked = { date ->
                    closePicker()
                    onIntent(NearbyIntent.DateChanged(date.toString()))
                },
                onDismiss = closePicker,
            )

        EventPicker.TIME ->
            EventTimePickerDialog(
                initial = draft.time(),
                onPicked = { time ->
                    closePicker()
                    onIntent(NearbyIntent.TimeChanged(time.toHourMinuteLabel()))
                },
                onDismiss = closePicker,
            )

        EventPicker.NONE -> Unit
    }
}

private enum class EventPicker { NONE, DATE, TIME }

/** Looks like [SheetField] but opens a picker instead of the keyboard. */
@Composable
private fun SheetPickerField(
    text: String,
    placeholder: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    Row(
        modifier =
            modifier
                .heightIn(min = PickerFieldHeight)
                .clip(SheetFieldShape)
                .background(SheetFieldFill)
                .semantics { contentDescription = text.ifBlank { placeholder } }
                .clickable(role = Role.Button) {
                    keyboard?.hide()
                    onClick()
                }.padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (text.isBlank()) SheetMutedText else ContentOnDark,
        )
        Text(
            text = text.ifBlank { placeholder },
            color = if (text.isBlank()) SheetMutedText else ContentOnDark,
            fontSize = 16.sp,
            maxLines = 1,
        )
    }
}

/**
 * Material3 date picker. The picker works in UTC midnight millis, so the chosen day is read
 * back in UTC and never shifts with the device zone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDatePickerDialog(
    initial: LocalDate?,
    onPicked: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState =
        rememberDatePickerState(
            initialSelectedDateMillis = initial?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds(),
        )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis
                        ?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date }
                        ?.let(onPicked) ?: onDismiss()
                },
            ) { Text(stringResource(Res.string.event_picker_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.event_picker_cancel)) }
        },
    ) {
        DatePicker(state = pickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventTimePickerDialog(
    initial: LocalTime?,
    onPicked: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState =
        rememberTimePickerState(
            initialHour = initial?.hour ?: DEFAULT_HOUR,
            initialMinute = initial?.minute ?: 0,
            is24Hour = DateFormat.is24HourFormat(LocalContext.current),
        )
    TimePickerDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.event_time_pick)) },
        confirmButton = {
            TextButton(onClick = { onPicked(LocalTime(pickerState.hour, pickerState.minute)) }) {
                Text(stringResource(Res.string.event_picker_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.event_picker_cancel)) }
        },
    ) {
        TimePicker(state = pickerState)
    }
}

private val PickerFieldHeight = 56.dp
private const val DATE_WEIGHT = 1.25f
private const val ERROR_ANIM_MILLIS = 200
private const val DEFAULT_HOUR = 9
