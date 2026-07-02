package bose.ankush.payment.di

import bose.ankush.payment.data.PaymentRepositoryImpl
import bose.ankush.payment.domain.repository.PaymentRepository
import bose.ankush.payment.domain.usecase.CreateOrderUseCase
import bose.ankush.payment.domain.usecase.VerifyPaymentUseCase
import bose.ankush.payment.presentation.PaymentViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin module for the payment feature — domain and data layer bindings.
 * Platform-specific bindings (NetworkConnectivity, PaymentApiService, PremiumStore,
 * PaymentConfig) must be provided by the host application.
 */
val paymentDomainModule: Module =
    module {
        single<PaymentRepository> { PaymentRepositoryImpl(get(), get()) }
        factory { CreateOrderUseCase(get()) }
        factory { VerifyPaymentUseCase(get()) }
    }

/** Koin module for the payment feature — presentation layer bindings. */
val paymentViewModelModule: Module =
    module {
        viewModelOf(::PaymentViewModel)
    }
