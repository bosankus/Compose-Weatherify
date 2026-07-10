import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "bose.ankush.navigation"
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
            baseName = "navigation"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":common-ui"))
            implementation(project(":feature:auth"))
            implementation(project(":feature:language"))
            implementation(project(":feature:payment"))
            implementation(project(":feature:finder"))
            implementation(project(":feature:home"))
            implementation(project(":feature:settings"))
            implementation(libs.compose.multiplatform.resources)
            implementation(libs.compose.multiplatform.runtime)
            implementation(libs.compose.multiplatform.foundation)
            implementation(libs.compose.multiplatform.material3)
            implementation(libs.compose.multiplatform.ui)
            implementation(libs.compose.multiplatform.animation)
            implementation(libs.compose.multiplatform.materialIconsExtended)
            implementation(libs.androidx.navigation3.runtime)
            implementation(libs.androidx.lifecycle.viewmodel.navigation3)
            implementation(libs.androidx.lifecycle.viewmodel.kmp)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }

        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
            // navigation3-ui (NavDisplay) has no iOS artifact yet; the Android actual of
            // AppNavHost uses it directly, iOS gets a minimal custom renderer instead.
            implementation(libs.androidx.navigation3.ui)
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

compose.resources {
    packageOfResClass = "bose.ankush.navigation.generated.resources"
}
