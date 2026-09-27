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
 * `PaymentConfig` (needs BuildConfig, which is per-app/per-build-variant) must still be
 * provided by the host application; `NetworkConnectivity`/`PaymentApiService`/
 * `PremiumStorage` are resolvable from `:network`/`:storage`'s own platform Koin modules.
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
