@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

// Prefix of the `swiftPMImport.<group>.<module>.*` namespace the SwiftPM cinterop bindings are
// generated under (see HomePlatformModule.kt / FirebaseHomeRemoteConfigGate.kt) — keep in sync
// with the imports there. Compose resource accessors are unaffected: this module pins its own
// package via `compose.resources { packageOfResClass = ... }` below.
group = "bose.ankush"


kotlin {
    // AGP 9's com.android.kotlin.multiplatform.library plugin implies the Android target itself —
    // androidTarget() is no longer needed (and conflicts with this plugin); configure it via android { }.
    android {
        namespace = "bose.ankush.home"
        compileSdk =
            libs.versions.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
        androidResources.enable = true
        // Enables running commonTest on the Android host (JVM unit tests).
        withHostTest {}
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    iosArm64()
    iosSimulatorArm64()


    // Firebase for iOS comes from the firebase-ios-sdk Swift package (the Kotlin CocoaPods plugin
    // is in maintenance mode). No framework {} is declared here: this module is linked into the
    // app through the :app:iosApp ComposeApp framework, never consumed as a standalone binary.
    swiftPMDependencies {
        iosMinimumDeploymentTarget = "16.0"

        // Firebase pulls in C++ transitive modules (gRPC, abseil, leveldb, BoringSSL) whose Clang
        // modules break cinterop generation, so auto-discovery is off and every module imported
        // from Kotlin is listed explicitly. RemoteConfig's Objective-C headers are vended by
        // FirebaseRemoteConfigInternal, not by the FirebaseRemoteConfig product module.
        discoverClangModulesImplicitly = false

        swiftPackage(
            url = url("https://github.com/firebase/firebase-ios-sdk.git"),
            version = exact("12.4.0"),
            products =
                listOf(
                    product("FirebaseCore"),
                    product("FirebaseRemoteConfig"),
                ),
            importedClangModules =
                listOf(
                    "FirebaseCore",
                    "FirebaseRemoteConfigInternal",
                ),
        )
    }


    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":network"))
                implementation(project(":storage"))
                implementation(project(":common-ui"))
                implementation(project(":analytics"))
                implementation(libs.compose.multiplatform.resources)
                implementation(libs.compose.multiplatform.runtime)
                implementation(libs.compose.multiplatform.foundation)
                implementation(libs.compose.multiplatform.material3)
                implementation(libs.compose.multiplatform.ui)
                implementation(libs.compose.multiplatform.ui.tooling.preview)
                implementation(libs.compose.multiplatform.animation)
                implementation(libs.compose.multiplatform.materialIconsExtended)
                implementation(libs.koin.core)
                implementation(libs.koin.core.viewmodel)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.androidx.lifecycle.viewmodel.kmp)
                implementation(libs.androidx.lifecycle.viewmodel.compose)
                implementation(libs.coil3.compose)
                implementation(libs.coil3.network.ktor)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.google.play.services.location)
                implementation(libs.koin.android)
                implementation(libs.firebase.config)
                implementation(libs.androidx.compose.ui.tooling)

                // Wearable Data Layer — pushes the fetched forecast to a paired Wear OS watch.
                implementation(libs.google.play.services.wearable)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.play.services)
                implementation(libs.timber)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val iosMain by creating {
            dependsOn(commonMain)
        }

        iosArm64Main.get().dependsOn(iosMain)
        iosSimulatorArm64Main.get().dependsOn(iosMain)

        val iosTest by creating {
            dependsOn(commonTest)
        }

        iosArm64Test.get().dependsOn(iosTest)
        iosSimulatorArm64Test.get().dependsOn(iosTest)
    }
}

compose.resources {
    packageOfResClass = "bose.ankush.home.generated.resources"
}

dependencies {
    add("androidMainImplementation", platform(libs.firebase.bom))
}
