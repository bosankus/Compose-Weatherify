import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    android {
        namespace = "bose.ankush.commonui"
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

        androidResources.enable = true
    }

    // iosX64 (Intel simulator) dropped: Compose Multiplatform stopped publishing artifacts for it
    // starting at 1.11.0, following Apple's deprecation of the x86_64 iOS Simulator.
    iosArm64()
    iosSimulatorArm64()

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        binaries.framework {
            baseName = "common_ui"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.compose.multiplatform.runtime)
                implementation(libs.compose.multiplatform.foundation)
                implementation(libs.compose.multiplatform.material3)
                implementation(libs.compose.multiplatform.ui)
                implementation(libs.compose.multiplatform.materialIconsExtended)
                implementation(libs.compose.multiplatform.ui.tooling.preview)
            }
        }

        val androidMain by getting {
            dependencies {
                // BackHandler support for InAppWebView
                implementation(libs.androidx.activity.compose)
                // WindowCompat/WindowInsetsControllerCompat for WeatherifyTheme system-bar styling
                implementation(libs.androidx.core.ktx)
            }
        }

        val iosMain by creating {
            dependsOn(commonMain)
        }

        @Suppress("UNUSED_VARIABLE")
        val iosArm64Main by getting {
            dependsOn(iosMain)
        }

        @Suppress("UNUSED_VARIABLE")
        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }
    }
}
