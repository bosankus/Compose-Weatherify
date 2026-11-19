package bose.ankush.auth.di

import bose.ankush.auth.backdrop.LoginBackdropViewModel
import bose.ankush.auth.presentation.AuthViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authViewModelModule: Module =
    module {
        viewModelOf(::AuthViewModel)
        // ForestPhotoStore is bound per platform in authDomainModule.
        viewModel { LoginBackdropViewModel(get(), get()) }
    }
