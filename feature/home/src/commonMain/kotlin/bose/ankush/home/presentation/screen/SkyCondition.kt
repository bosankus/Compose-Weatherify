package bose.ankush.home.presentation.screen

/**
 * Coarse sky for the home backdrop. [key] names the bundled photo and the persisted Unsplash
 * pick; [line] is the header fallback when the forecast has no description.
 */
enum class SkyCondition(
    val key: String,
    val line: String,
) {
    CLEAR("clear", "It's clear"),
    CLOUDS("clouds", "It's cloudy"),
    RAIN("rain", "It's raining"),
    FOG("fog", "It's foggy"),
    STORM("storm", "It's stormy"),
    SNOW("snow", "It's snowing"),
    NIGHT("night", "It's night"),
}
