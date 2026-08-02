import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    id("org.jetbrains.kotlin.native.cocoapods")
}

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
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        version = "1.0"
        summary = "Weatherify Home feature module"
        homepage = "https://github.com/bosankus/Compose-Weatherify"
        ios.deploymentTarget = "15.0"
        framework {
            baseName = "feature_home"
            isStatic = true
        }

        pod("FirebaseCore") {
            version = "12.4.0"
        }

        pod("FirebaseRemoteConfig") {
            version = "12.4.0"
            extraOpts += listOf("-compiler-option", "-fmodules")
        }
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

        val iosMain by creating {
            dependsOn(commonMain)
        }

        iosArm64Main.get().dependsOn(iosMain)
        iosSimulatorArm64Main.get().dependsOn(iosMain)
    }
}

compose.resources {
    packageOfResClass = "bose.ankush.home.generated.resources"
}

dependencies {
    add("androidMainImplementation", platform(libs.firebase.bom))
}
