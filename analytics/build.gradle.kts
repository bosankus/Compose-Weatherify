@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

// Prefix of the `swiftPMImport.<group>.<module>.*` namespace the SwiftPM cinterop bindings are
// generated under (see FirebaseAnalyticsTracker.kt) — keep in sync with the imports there.
group = "bose.ankush"

kotlin {
    // AGP 9's com.android.kotlin.multiplatform.library plugin implies the Android target itself —
    // androidTarget() is no longer needed (and conflicts with this plugin); configure it via android { }.
    android {
        namespace = "bose.ankush.analytics"
        compileSdk =
            libs.versions.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
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
        // modules break cinterop generation, so auto-discovery is off and only the module actually
        // imported from Kotlin is listed. FirebaseCore's bindings live in :feature:home — declaring
        // the same Clang module in two modules of one dependency chain generates it only once.
        discoverClangModulesImplicitly = false

        swiftPackage(
            url = url("https://github.com/firebase/firebase-ios-sdk.git"),
            version = exact("12.4.0"),
            products = listOf(product("FirebaseAnalytics")),
            importedClangModules = listOf("FirebaseAnalytics"),
        )
    }

    sourceSets {
        val commonMain =
            getByName("commonMain") {
                dependencies {
                    implementation(libs.koin.core)
                    implementation(libs.kotlinx.coroutines.core)
                }
            }

        getByName("androidMain") {
            dependencies {
                implementation(libs.koin.android)
            }
        }

        val iosMain =
            create("iosMain") {
                dependsOn(commonMain)
            }

        iosArm64Main.get().dependsOn(iosMain)
        iosSimulatorArm64Main.get().dependsOn(iosMain)
    }
}

dependencies {
    add("androidMainImplementation", platform(libs.firebase.bom))
    add("androidMainImplementation", libs.firebase.analytics)
}
