import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
    id("dagger.hilt.android.plugin")
    id("kotlin-parcelize")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    id("com.github.ben-manes.versions")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    compileSdk = ConfigData.compileSdkVersion

    defaultConfig {
        applicationId = "bose.ankush.weatherify"
        minSdk = ConfigData.minSdkVersion
        targetSdk = ConfigData.targetSdkVersion
        versionCode = ConfigData.versionCode
        versionName = ConfigData.versionName
        multiDexEnabled = ConfigData.multiDexEnabled
        testInstrumentationRunner = "bose.ankush.weatherify.helper.HiltTestRunner"
        @Suppress("UnstableApiUsage")
        androidResources {
            localeFilters.addAll(listOf("en", "hi", "iw"))
        }
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
                "proguard-rules.pro"
            )
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
}


ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {

    api(project(":language"))
    api(project(":storage"))
    api(project(":network"))
    api(project(":sunriseui"))

    // Core
    implementation(Deps.androidCore)
    implementation(Deps.appCompat)
    implementation(Deps.androidMaterial)
    implementation(Deps.viewModelCompose)
    implementation(Deps.navigationCompose)
    implementation(Deps.inAppUpdate)
    implementation(Deps.inAppUpdateKtx)
    implementation(Deps.googlePlayLocation)
    implementation(Deps.systemUIController)
    implementation(Deps.composePermission)
    implementation(Deps.dataStore)
    implementation(Deps.splashScreen)


    // Compose
    implementation(platform(Deps.composeBom))
    implementation(Deps.composeUi)
    debugImplementation(Deps.composeUiTooling)
    implementation(Deps.composeUiToolingPreview)
    implementation(Deps.composeMaterial3)
    implementation(Deps.composeIconsExtended)

    // Unit Testing
    testImplementation(Deps.junit)
    testImplementation(Deps.truth)
    testImplementation(Deps.turbine)
    testImplementation(Deps.coroutineTest)
    testImplementation(Deps.coreTesting)
    testImplementation(Deps.mockitoInline)
    testImplementation(Deps.mockitoNhaarman)
    testImplementation(Deps.mockWebServer)
    testImplementation(Deps.mockk)

    // UI Testing
    androidTestImplementation(Deps.extJunit)
    androidTestImplementation(Deps.espressoCore)
    androidTestImplementation(Deps.espressoContrib)
    androidTestImplementation(Deps.hiltTesting)
    kspAndroidTest(Deps.hiltDaggerAndroidCompiler)

    // Networking
    implementation(Deps.gson)

    // Room runtime for providing WeatherDatabase from app DI
    implementation(Deps.room)
    implementation(Deps.roomKtx)

    // Firebase
    implementation(platform(Deps.firebaseBom))
    implementation("com.google.firebase:firebase-config")
    implementation("com.google.firebase:firebase-analytics")
    implementation(Deps.firebasePerformanceMonitoring)
    implementation("com.google.firebase:firebase-messaging")

    // Coroutines
    implementation(Deps.coroutinesCore)
    implementation(Deps.coroutinesAndroid)

    // Dependency Injection
    implementation(Deps.hilt)
    implementation(Deps.hiltNavigationCompose)
    ksp(Deps.hiltDaggerAndroidCompiler)
    ksp(Deps.hiltAndroidXCompiler)

    // Miscellaneous
    implementation(Deps.timber)
    // Removed Lottie dependency as per requirements
    implementation(Deps.coilCompose)

    // Memory leak
    debugImplementation(Deps.leakCanary)

    // Payment SDK moved to app module
    implementation(Deps.razorPay)
}


kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xopt-in=androidx.compose.animation.ExperimentalAnimationApi"
        )
    }
}
