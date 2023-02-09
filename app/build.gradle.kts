plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
    id("kotlin-parcelize")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    id("com.github.ben-manes.versions")
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
        testInstrumentationRunner = "bose.ankush.weatherify.helper.HiltTestRunner"
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
        jvmTarget = JavaVersion.VERSION_11.toString()
    }
}

dependencies {

    api(project(":Utilities"))
    api(project(":Dialog"))

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
    androidTestImplementation(Deps.composeUiTestJunit4)
    androidTestImplementation(Deps.hiltTesting)
    debugImplementation(Deps.composeUiTestManifest)
    kaptAndroidTest(Deps.hiltDaggerAndroidCompiler)

    // Compose
    implementation(Deps.composeUi)
    implementation(Deps.composeMaterial)
    debugImplementation(Deps.composeUiTooling)
    implementation(Deps.composeUiToolingPreview)

    // Core
    implementation(Deps.androidCore)
    implementation(Deps.appCompat)
    implementation(Deps.androidMaterial)
    implementation(Deps.viewModelCompose)
    implementation(Deps.navigationCompose)
    implementation(Deps.inAppUpdate)
    implementation(Deps.inAppUpdateKtx)
    implementation(Deps.googlePlayLocation)
    implementation(Deps.animatedNavigation)
    implementation(Deps.composePermission)

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
    implementation(Deps.hiltNavigationCompose)
    kapt(Deps.hiltDaggerAndroidCompiler)

    // Miscellaneous
    implementation(Deps.timber)
    implementation(Deps.lottieCompose)
    implementation(Deps.coilCompose)

    // Debug bridge
    implementation(Deps.stetho)

    // Memory leak
    debugImplementation(Deps.leakCanary)
}