import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    id("org.jetbrains.kotlin.native.cocoapods")
}

kotlin {
    cocoapods {
        version = "1.0"
        summary = "Weatherify Home feature module"
        homepage = "https://github.com/bosankus/Compose-Weatherify"
        ios.deploymentTarget = "15.0"

        pod("FirebaseCore") {
            version = "12.4.0"
        }

        pod("FirebaseRemoteConfig") {
            version = "12.4.0"
            extraOpts += listOf("-compiler-option", "-fmodules")
        }
    }

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

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }

        androidResources.enable = true
    }

    iosArm64()
    iosSimulatorArm64()

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        binaries.framework {
            baseName = "feature_home"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":network"))
            implementation(project(":storage"))
            implementation(project(":common-ui"))
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

        androidMain.dependencies {
            implementation(libs.google.play.services.location)
            implementation(libs.koin.android)
            implementation(libs.firebase.config)
            implementation(libs.androidx.compose.ui.tooling)
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
    packageOfResClass = "bose.ankush.home.generated.resources"
}

dependencies {
    add("androidMainImplementation", platform(libs.firebase.bom))
}
