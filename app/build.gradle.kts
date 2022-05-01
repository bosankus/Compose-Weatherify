plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("androidx.navigation.safeargs.kotlin")
    id("dagger.hilt.android.plugin")
    id("kotlin-parcelize")
    id("com.google.secrets_gradle_plugin") version "0.6.1"
}

android {
    compileSdk = ConfigData.compileSdkVersion
    buildToolsVersion = ConfigData.buildToolsVersion

    defaultConfig {
        applicationId = "bose.ankush.weatherify"
        minSdk = ConfigData.minSdkVersion
        targetSdk = ConfigData.targetSdkVersion
        versionCode = ConfigData.versionCode
        versionName = ConfigData.versionName
        multiDexEnabled = ConfigData.multiDexEnabled
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        dataBinding = true
        viewBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = Versions.composeVersion
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    api(project(":Utilities"))

    // Unit Testing
    testImplementation(Deps.junit)
    testImplementation(Deps.truth)
    testImplementation(Deps.turbine)
    testImplementation(Deps.coroutineTest)
    testImplementation(Deps.coreTesting)
    testImplementation(Deps.mockitoInline)
    testImplementation(Deps.mockitoNhaarman)
    testImplementation(Deps.mockWebServer)

    // UI Testing
    androidTestImplementation(Deps.extJunit)
    androidTestImplementation(Deps.espressoCore)
    androidTestImplementation(Deps.espressoContrib)
    androidTestImplementation(Deps.composeUiTest)

    // Compose
    implementation(Deps.composeUi)
    implementation(Deps.composeMaterial)
    debugImplementation(Deps.composeUiTooling)
    implementation(Deps.composeUiToolingPreview)

    // Core
    implementation(Deps.androidCore)
    implementation(Deps.appCompat)
    implementation(Deps.multiDex)
    implementation(Deps.androidMaterial)
    implementation(Deps.constraintLayout)
    implementation(Deps.recyclerView)
    implementation(Deps.fragment)
    implementation(Deps.viewModel)
    implementation(Deps.viewModelCompose)
    implementation(Deps.navigationFragment)
    implementation(Deps.navigationUi)
    implementation(Deps.navigationCompose)

    // Networking
    implementation(Deps.okHttp3)
    implementation(Deps.retrofit)
    implementation(Deps.retrofitGson)
    implementation(Deps.okhttpInterceptor)

    // Coroutines
    implementation(Deps.retrofitCoroutineAdapter)
    implementation(Deps.coroutinesCore)
    implementation(Deps.coroutinesAndroid)

    // Dependency Injection
    implementation(Deps.hilt)
    kapt(Deps.hiltDaggerAndroidCompiler)
    kapt(Deps.hiltAndroidxCompiler)
    implementation(Deps.hiltNavigationCompose)

    // Miscellaneous
    implementation(Deps.timber)
    implementation(Deps.lottie)
    implementation(Deps.lottieCompose)
    implementation(Deps.glide)
    implementation(Deps.coilCompose)
}