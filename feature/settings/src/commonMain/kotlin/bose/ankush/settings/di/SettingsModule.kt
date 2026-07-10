package bose.ankush.settings.di

import bose.ankush.settings.presentation.SettingsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Relies on `networkDomainModule` (module `:network`) being loaded for
 * `bose.ankush.network.repository.ServiceRepository`. */
val settingsViewModelModule: Module =
    module {
        viewModelOf(::SettingsViewModel)
    }
