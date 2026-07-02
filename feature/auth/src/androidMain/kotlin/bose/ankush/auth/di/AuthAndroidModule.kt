package bose.ankush.auth.di

import bose.ankush.auth.presentation.AuthViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authViewModelModule: Module =
    module {
        viewModelOf(::AuthViewModel)
    }
