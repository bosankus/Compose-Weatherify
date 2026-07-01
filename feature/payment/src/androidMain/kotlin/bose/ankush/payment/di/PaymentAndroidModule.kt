package bose.ankush.payment.di

import bose.ankush.payment.presentation.PaymentViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val paymentViewModelModule: Module =
    module {
        viewModelOf(::PaymentViewModel)
    }
