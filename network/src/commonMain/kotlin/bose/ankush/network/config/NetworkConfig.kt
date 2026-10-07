package bose.ankush.network.config

/**
 * Build-time values (API keys etc.) the network layer needs but can't read itself: they live in
 * per-app build config (Android BuildConfig, iOS IosSecrets), which a KMP library can't see.
 * Each app binds an implementation in its own Koin graph.
 *
 * Add one typed property per value rather than a string-keyed lookup, so a missing value is a
 * compile error in every host app instead of a runtime miss.
 */
interface NetworkConfig {
    val unsplashAccessKey: String
}
