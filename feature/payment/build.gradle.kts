import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    android {
        namespace = "bose.ankush.payment"
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

    // iosX64 dropped: Compose Multiplatform stopped publishing artifacts for it
    // starting at 1.11.0, following Apple's deprecation of the x86_64 iOS Simulator.
    iosArm64()
    iosSimulatorArm64()

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        binaries.framework {
            baseName = "feature_payment"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":network"))
            implementation(libs.compose.multiplatform.resources)
            implementation(libs.compose.multiplatform.runtime)
            implementation(libs.koin.core)
            implementation(libs.koin.core.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.androidx.lifecycle.viewmodel.kmp)
            implementation(libs.kotlinx.serialization.json)
        }

        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting

        @Suppress("UNUSED_VARIABLE")
        val iosMain by creating {
            dependsOn(commonMain.get())
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
    }
}

compose.resources {
    packageOfResClass = "bose.ankush.payment.generated.resources"
}
