package bose.ankush.home.presentation.shell

internal object ShellReducer {
    fun reduce(
        state: ShellState,
        intent: ShellIntent,
    ): ShellReduce =
        when (intent) {
            is ShellIntent.SelectTab ->
                when (intent.tab) {
                    ShellTab.SETTINGS -> ShellReduce(state, ShellEffect.OpenSettings)
                    else -> ShellReduce(state.copy(tab = intent.tab))
                }

            ShellIntent.OpenAccount -> ShellReduce(state, ShellEffect.OpenSettings)

            is ShellIntent.LocationUpdated ->
                ShellReduce(
                    state.copy(
                        lat = intent.lat,
                        lon = intent.lon,
                        featuredPlace = null,
                        eventDates = emptySet(),
                    ),
                )

            is ShellIntent.PlaceNameUpdated -> ShellReduce(state.copy(placeName = intent.placeName))

            ShellIntent.OpenCreate -> ShellReduce(state.copy(showCreate = true, createError = null))

            ShellIntent.DismissCreate ->
                ShellReduce(state.copy(showCreate = false, posting = false, createError = null))

            is ShellIntent.TitleChanged -> ShellReduce(state.copy(title = intent.value))

            is ShellIntent.DateChanged -> ShellReduce(state.copy(dateText = intent.value))

            is ShellIntent.TimeChanged -> ShellReduce(state.copy(timeText = intent.value))

            is ShellIntent.SideDataLoaded ->
                ShellReduce(state.copy(featuredPlace = intent.featured, eventDates = intent.eventDates))

            is ShellIntent.AccountLoaded -> ShellReduce(state.copy(photoUrl = intent.photoUrl))

            is ShellIntent.CreateFailed ->
                ShellReduce(state.copy(posting = false, createError = intent.message))

            ShellIntent.CreateSucceeded ->
                ShellReduce(
                    state.copy(
                        showCreate = false,
                        posting = false,
                        title = "",
                        dateText = "",
                        timeText = "",
                        createError = null,
                    ),
                )
        }
}
