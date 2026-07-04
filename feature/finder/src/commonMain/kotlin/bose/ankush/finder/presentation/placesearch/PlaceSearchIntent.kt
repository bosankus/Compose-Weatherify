package bose.ankush.finder.presentation.placesearch

internal sealed interface PlaceSearchIntent {
    data class QueryChanged(
        val query: String,
    ) : PlaceSearchIntent

    object Clear : PlaceSearchIntent
}
