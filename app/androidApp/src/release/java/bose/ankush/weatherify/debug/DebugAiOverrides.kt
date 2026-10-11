package bose.ankush.weatherify.debug

import org.koin.core.module.Module

/** Release builds have no demo fallback: the real Gemini Nano client, or no card. */
object DebugAiOverrides {
    fun modules(): List<Module> = emptyList()
}
