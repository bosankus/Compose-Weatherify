import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

android {
    namespace = "bose.ankush.weatherify.wear"
    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()

    defaultConfig {
        // Must match the phone app's applicationId exactly: the Wear Data Layer only
        // delivers DataItems between apps with the same package name + signing cert.
        applicationId = "bose.ankush.weatherify"
        // Wear OS 3+ (the only actively supported line) requires API 30+, above this
        // project's phone minSdk of 28 — hardcoded here rather than via the shared catalog entry.
        minSdk = 30
        targetSdk = 36
        versionCode = ConfigData.versionCode
        versionName = ConfigData.versionName
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = false
    }
}

dependencies {
    // Shared KMM business logic — same modules the phone app consumes.
    api(project(":network"))
    api(project(":storage"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.activity.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)

    // Wear-specific UI toolkit — NOT interchangeable with phone Compose Material3.
    implementation(libs.androidx.wear.compose.material3)
    implementation(libs.androidx.wear.compose.foundation)
    implementation(libs.androidx.wear.compose.navigation3)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.wear.tooling.preview)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)

    // Wearable Data Layer API — phone<->watch messaging/data sync.
    implementation(libs.google.play.services.wearable)
    implementation(libs.kotlinx.coroutines.play.services)

    // Weather condition icons (sun/cloud/rain/etc.)
    implementation(libs.androidx.compose.material.icons.extended)

    // Weather condition animations (e.g. rain) — plays a Lottie JSON from res/raw.
    implementation(libs.lottie.compose)

    implementation(libs.timber)
}
