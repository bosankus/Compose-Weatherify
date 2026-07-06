package bose.ankush.home

/**
 * Cross-feature entry point for pinning a saved location as Home's active weather source.
 *
 * `feature:home`'s own [bose.ankush.home.presentation.home.HomeFeatureRoute] observes the same
 * underlying preferences reactively, so calling this from a sibling tab (e.g. saved locations)
 * is picked up by Home next time its route is composed — no direct ViewModel reference needed
 * across module boundaries.
 */
interface HomeLocationCoordinator {
    suspend fun setDefaultLocation(
        lat: Double,
        lon: Double,
        name: String,
    )
}
