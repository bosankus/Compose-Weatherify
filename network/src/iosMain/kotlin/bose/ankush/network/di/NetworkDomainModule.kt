package bose.ankush.network.di

import bose.ankush.network.util.IOSNetworkConnectivity
import bose.ankush.network.util.NetworkConnectivity
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.dsl.module

actual val networkDomainModule: Module = module {
    single<NetworkConnectivity> { IOSNetworkConnectivity() }
    single<HttpClient> { createHttpClient() }
}
