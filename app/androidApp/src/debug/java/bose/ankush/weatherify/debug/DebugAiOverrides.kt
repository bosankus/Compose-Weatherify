package bose.ankush.weatherify.debug

import bose.ankush.home.data.ai.createOnDeviceAiClient
import bose.ankush.home.domain.ai.OnDeviceAiClient
import org.koin.core.module.Module
import org.koin.dsl.module

/** Debug builds only: on a phone without Gemini Nano, the weather summary runs on demo text. */
object DebugAiOverrides {
    fun modules(): List<Module> =
        listOf(module { single<OnDeviceAiClient> { DemoFallbackAiClient(createOnDeviceAiClient()) } })
}
