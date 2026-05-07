package bose.ankush.payment.di

import bose.ankush.payment.presentation.PaymentViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val paymentViewModelModule: Module =
    module {
        viewModel { PaymentViewModel(get(), get(), get(), get()) }
    }

/**
 * All Koin modules required by the feature-payment module on Android.
 * Load these in the host application's [org.koin.core.context.startKoin] call,
 * alongside the app-level module that provides the platform-specific bindings:
 * [bose.ankush.network.api.PaymentApiService], [bose.ankush.network.common.NetworkConnectivity],
 * [bose.ankush.payment.domain.store.PremiumStore], and [bose.ankush.payment.domain.config.PaymentConfig].
 */
val featurePaymentModules: List<Module> = listOf(paymentDomainModule, paymentViewModelModule)
