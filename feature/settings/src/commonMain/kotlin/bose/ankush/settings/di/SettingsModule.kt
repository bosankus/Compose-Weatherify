package bose.ankush.settings.di

import bose.ankush.network.auth.token.TokenManager
import bose.ankush.settings.domain.DeleteAccount
import bose.ankush.settings.domain.ObserveAccount
import bose.ankush.settings.domain.RefreshAccount
import bose.ankush.settings.domain.RemoveProfilePhoto
import bose.ankush.settings.domain.UploadProfilePhoto
import bose.ankush.settings.presentation.SettingsViewModel
import bose.ankush.settings.presentation.profile.ProfileViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Relies on `networkDomainModule` (module `:network`) being loaded for
 * `ServiceRepository`, `AccountRepository`, and `TokenManager`. */
val settingsViewModelModule: Module =
    module {
        factory { ObserveAccount(get()) }
        factory { RefreshAccount(get()) }
        factory { UploadProfilePhoto(get()) }
        factory { RemoveProfilePhoto(get()) }
        factory { DeleteAccount(get(), get<TokenManager>()) }
        viewModelOf(::SettingsViewModel)
        viewModelOf(::ProfileViewModel)
    }
