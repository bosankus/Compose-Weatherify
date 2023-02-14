package bose.ankush.weatherify.domain.model

data class CityName(
    val name: String?
) {
    fun doesMatchSearchQuery(query: String): Boolean {
        val matchingCombinations = listOf(
            "$name",
            "${name?.first()}",
            "${name?.last()}"
        )
        return matchingCombinations.any {
            it.contains(query, ignoreCase = true)
        }
    }
}
