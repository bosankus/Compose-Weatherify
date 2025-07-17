package bose.ankush.sunriseui.components

enum class WeatherCondition(val description: String) {
    // Group 2xx: Thunderstorm
    THUNDERSTORM_WITH_LIGHT_RAIN("thunderstorm with light rain"),
    THUNDERSTORM_WITH_RAIN("thunderstorm with rain"),
    THUNDERSTORM_WITH_HEAVY_RAIN("thunderstorm with heavy rain"),
    LIGHT_THUNDERSTORM("light thunderstorm"),
    THUNDERSTORM("thunderstorm"),
    HEAVY_THUNDERSTORM("heavy thunderstorm"),
    RAGGED_THUNDERSTORM("ragged thunderstorm"),
    THUNDERSTORM_WITH_LIGHT_DRIZZLE("thunderstorm with light drizzle"),
    THUNDERSTORM_WITH_DRIZZLE("thunderstorm with drizzle"),
    THUNDERSTORM_WITH_HEAVY_DRIZZLE("thunderstorm with heavy drizzle"),

    // Group 3xx: Drizzle
    LIGHT_INTENSITY_DRIZZLE("light intensity drizzle"),
    DRIZZLE("drizzle"),
    HEAVY_INTENSITY_DRIZZLE("heavy intensity drizzle"),
    LIGHT_INTENSITY_DRIZZLE_RAIN("light intensity drizzle rain"),
    DRIZZLE_RAIN("drizzle rain"),
    HEAVY_INTENSITY_DRIZZLE_RAIN("heavy intensity drizzle rain"),
    SHOWER_RAIN_AND_DRIZZLE("shower rain and drizzle"),
    HEAVY_SHOWER_RAIN_AND_DRIZZLE("heavy shower rain and drizzle"),
    SHOWER_DRIZZLE("shower drizzle"),

    // Group 5xx: Rain
    LIGHT_RAIN("light rain"),
    MODERATE_RAIN("moderate rain"),
    HEAVY_INTENSITY_RAIN("heavy intensity rain"),
    VERY_HEAVY_RAIN("very heavy rain"),
    EXTREME_RAIN("extreme rain"),
    FREEZING_RAIN("freezing rain"),
    LIGHT_INTENSITY_SHOWER_RAIN("light intensity shower rain"),
    SHOWER_RAIN("shower rain"),
    HEAVY_INTENSITY_SHOWER_RAIN("heavy intensity shower rain"),
    RAGGED_SHOWER_RAIN("ragged shower rain"),

    // Group 6xx: Snow
    LIGHT_SNOW("light snow"),
    SNOW("snow"),
    HEAVY_SNOW("heavy snow"),
    SLEET("sleet"),
    LIGHT_SHOWER_SLEET("light shower sleet"),
    SHOWER_SLEET("shower sleet"),
    LIGHT_RAIN_AND_SNOW("light rain and snow"),
    RAIN_AND_SNOW("rain and snow"),
    LIGHT_SHOWER_SNOW("light shower snow"),
    SHOWER_SNOW("shower snow"),
    HEAVY_SHOWER_SNOW("heavy shower snow"),

    // Group 7xx: Atmosphere
    MIST("mist"),
    SMOKE("smoke"),
    HAZE("haze"),
    SAND_DUST_WHIRLS("sand/dust whirls"),
    FOG("fog"),
    SAND("sand"),
    DUST("dust"),
    VOLCANIC_ASH("volcanic ash"),
    SQUALLS("squalls"),
    TORNADO("tornado"),

    // Group 800: Clear
    CLEAR_SKY("clear sky"),

    // Group 80x: Clouds
    FEW_CLOUDS("few clouds: 11-25%"),
    SCATTERED_CLOUDS("scattered clouds: 25-50%"),
    BROKEN_CLOUDS("broken clouds: 51-84%"),
    OVERCAST_CLOUDS("overcast clouds: 85-100%");

    override fun toString(): String = description
}