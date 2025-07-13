package bose.ankush.network.di

import bose.ankush.network.common.IOSNetworkConnectivity
import bose.ankush.network.common.NetworkConnectivity
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * iOS-specific network module
 */
fun iosNetworkModule(): Module = module {
    // iOS-specific NetworkConnectivity implementation
    single<NetworkConnectivity> {
        IOSNetworkConnectivity()
    }
}