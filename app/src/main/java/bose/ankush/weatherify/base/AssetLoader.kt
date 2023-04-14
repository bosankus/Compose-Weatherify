package bose.ankush.weatherify.base

import bose.ankush.weatherify.base.configuration.ConfigurationLoader
import bose.ankush.weatherify.base.configuration.RawStringConfigurationLoader
import bose.ankush.weatherify.base.configuration.requireConfig
import bose.ankush.weatherify.domain.model.Country

interface AssetLoader {

    fun getCountryConfiguration(): Country
}

class ApplicationAssetLoader(
   private val configurationLoader: ConfigurationLoader,
   private val rawStringConfigurationLoader: RawStringConfigurationLoader
): AssetLoader {

    override fun getCountryConfiguration(): Country = loadConfig(fileName = "countryConfig.json")

    private inline fun <reified T: Any> loadConfig(fileName: String): T {
        if (T::class == String::class) {
            return rawStringConfigurationLoader.loadRawString(fileName) as T
        }
        return configurationLoader.requireConfig(fileName)
    }
}