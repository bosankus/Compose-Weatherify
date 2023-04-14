package bose.ankush.weatherify.base.configuration

import kotlin.reflect.KClass

interface ConfigurationLoader {
    fun <T : Any> loadConfig(fileName: String, type: KClass<T>): T?
}

/**
 * Loads the configuration of the given type from the file with the provided name.
 * @param fileName The name of the file from which the configuration is to be loaded.
 * @return The loaded configuration of the given type.
 * @throws IllegalStateException If the configuration file with the given name does not exist.
 */
inline fun <reified T : Any> ConfigurationLoader.requireConfig(fileName: String): T =
    loadConfig(fileName, T::class)
        ?: throw IllegalStateException("$fileName configuration does not exists")