package bose.ankush.home

/**
 * Asks for a place-event sync from the host app (Android app start, iOS foreground). Returns at
 * once; the sync runs in the background, is skipped while signed out, and never runs twice at once.
 */
interface PlaceEventSyncTrigger {
    fun requestSync()
}
