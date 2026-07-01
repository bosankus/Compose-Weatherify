package bose.ankush.payment.di

import bose.ankush.payment.data.PaymentRepositoryImpl
import bose.ankush.payment.domain.repository.PaymentRepository
import bose.ankush.payment.domain.usecase.CreateOrderUseCase
import bose.ankush.payment.domain.usecase.VerifyPaymentUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module for the payment feature — domain and data layer bindings.
 * Platform-specific bindings (NetworkConnectivity, PaymentApiService, PremiumStore,
 * PaymentConfig) and the ViewModel must be provided by the host application.
 *
 * @see bose.ankush.payment.di — androidMain for Android ViewModel module.
 */
val paymentDomainModule: Module =
    module {
        single<PaymentRepository> { PaymentRepositoryImpl(get(), get()) }
        factory { CreateOrderUseCase(get()) }
        factory { VerifyPaymentUseCase(get()) }
    }
