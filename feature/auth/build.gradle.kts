import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    android {
        namespace = "bose.ankush.auth"
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

    // iosX64 dropped: Compose Multiplatform stopped publishing artifacts for it
    // starting at 1.11.0, following Apple's deprecation of the x86_64 iOS Simulator.
    iosArm64()
    iosSimulatorArm64()

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        binaries.framework {
            baseName = "feature_auth"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":network"))
            implementation(libs.compose.multiplatform.runtime)
            implementation(libs.compose.multiplatform.foundation)
            implementation(libs.compose.multiplatform.material3)
            implementation(libs.compose.multiplatform.ui)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.androidx.lifecycle.viewmodel.kmp)
            implementation(libs.kotlinx.datetime)
        }

        androidMain.dependencies {
            implementation(libs.koin.android)
        }

        val iosMain by creating {
            dependsOn(commonMain.get())
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
