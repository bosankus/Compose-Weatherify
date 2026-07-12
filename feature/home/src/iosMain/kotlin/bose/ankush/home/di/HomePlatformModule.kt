package bose.ankush.home.di

import bose.ankush.home.data.location.IosHomeGeocoder
import bose.ankush.home.data.location.IosLocationClient
import bose.ankush.home.data.remoteconfig.FirebaseHomeRemoteConfigGate
import bose.ankush.home.domain.location.HomeGeocoder
import bose.ankush.home.domain.location.LocationClient
import bose.ankush.home.domain.remoteconfig.HomeRemoteConfigGate
import cocoapods.FirebaseCore.FIRApp
import cocoapods.FirebaseCore.FIROptions
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSBundle

@OptIn(ExperimentalForeignApi::class)
fun initializeFirebase() {
    if (FIRApp.defaultApp() != null) return
    val plistPath = NSBundle.mainBundle.pathForResource("GoogleService-Info", ofType = "plist")
    if (plistPath == null) {
        println("GoogleService-Info.plist not found in bundle — skipping Firebase init")
        return
    }
    val options = FIROptions(contentsOfFile = plistPath)
    if (options == null) {
        println("GoogleService-Info.plist could not be parsed — skipping Firebase init")
        return
    }
    try {
        FIRApp.configureWithOptions(options)
    } catch (e: Exception) {
        println("Firebase configure failed — skipping: ${e.message}")
    }
}

actual val homePlatformModule: Module =
    module {
        single<LocationClient> { IosLocationClient() }
        single<HomeRemoteConfigGate> { FirebaseHomeRemoteConfigGate() }
        single<HomeGeocoder> { IosHomeGeocoder() }
    }
