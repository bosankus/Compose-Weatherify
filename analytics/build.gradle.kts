import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    id("org.jetbrains.kotlin.native.cocoapods")
}

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

    cocoapods {
        version = "1.0"
        summary = "Weatherify Analytics module"
        homepage = "https://github.com/bosankus/Compose-Weatherify"
        ios.deploymentTarget = "16.0"
        framework {
            baseName = "analytics"
            isStatic = true
        }

        pod("FirebaseAnalytics") {
            version = "12.4.0"
            extraOpts += listOf("-compiler-option", "-fmodules")
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.koin.core)
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.koin.android)
            }
        }

        val iosMain by creating {
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
