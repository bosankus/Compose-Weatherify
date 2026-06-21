import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.secrets.gradle.plugin)
    alias(libs.plugins.ben.manes.versions)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "bose.ankush.weatherify"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = ConfigData.versionCode
        versionName = ConfigData.versionName
        multiDexEnabled = ConfigData.multiDexEnabled
        testInstrumentationRunner = "bose.ankush.weatherify.helper.HiltTestRunner"
    }

    @Suppress("UnstableApiUsage")
    androidResources {
        localeFilters.addAll(listOf("en", "hi", "iw", "bn", "kn", "ml", "ta", "te"))
    }

    packaging {
        resources {
            excludes.add("META-INF/versions/9/previous-compilation-data.bin")
        }
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
            // Release signing config should be configured via gradle.properties or build command
            // e.g., -Pandroid.injected.signing.store.file=/path/to/release.keystore
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

    namespace = "bose.ankush.weatherify"
    kotlin {
        compilerOptions {
            freeCompilerArgs.add("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {

    api(project(":common-ui"))
    api(project(":feature-payment"))
    api(project(":language"))
    api(project(":storage"))
    api(project(":network"))

    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Navigation 3
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.google.play.app.update)
    implementation(libs.google.play.app.update.ktx)
    implementation(libs.google.play.services.location)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.core.splashscreen)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Unit Testing
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.mockito.inline)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.mockk)

    // UI Testing
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.espresso.contrib)
    androidTestImplementation(libs.google.dagger.hilt.android.testing)
    kspAndroidTest(libs.google.dagger.hilt.android.compiler)

    // Networking
    implementation(libs.gson)

    // Room runtime for providing WeatherDatabase from app DI
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Firebase - BOM
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.perf)
    implementation(libs.firebase.messaging)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Date/Time (KMP-compatible, replaces java.time)
    implementation(libs.kotlinx.datetime)

    // Dependency Injection
    implementation(libs.google.dagger.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.google.dagger.hilt.android.compiler)
    ksp(libs.androidx.hilt.compiler)

    // Miscellaneous
    implementation(libs.timber)
    implementation(libs.coil.compose)

    // Memory leak
    debugImplementation(libs.leakcanary.android)

    // Payment SDK (Android-only — Razorpay checkout is launched from the app layer)
    implementation(libs.razorpay.checkout)

    // Koin — bridges the feature-payment Koin module with Hilt-managed singletons
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
        )
    }
}

// com.razorpay:checkout:1.6.41 pulls in standard-core via a dynamic "latest.integration" version,
// which resolves to releases that split out com.razorpay:core as a separate artifact while keeping
// the same com.razorpay namespace on both — AGP rejects that as a duplicate namespace. Pin to the
// last version before the split.
configurations.all {
    resolutionStrategy {
        force("com.razorpay:standard-core:1.6.56")
    }
}
