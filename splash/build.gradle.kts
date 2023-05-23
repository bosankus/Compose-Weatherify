plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

@Suppress("UnstableApiUsage")
android {
    namespace = "bose.ankush.splash"
    compileSdk = ConfigData.compileSdkVersion

    defaultConfig {
        minSdk = ConfigData.minSdkVersion

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    composeOptions {
        kotlinCompilerExtensionVersion = Versions.compilerExtensionVersion
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    kotlin {
        sourceSets.all {
            languageSettings {
                languageVersion = Versions.kotlinCompiler
            }
        }
    }

    lint {
        abortOnError = false
    }
}

dependencies {

    // Testing
    testImplementation(Deps.junit)

    // UI Testing
    androidTestImplementation(Deps.extJunit)

    // Core
    implementation(Deps.androidCore)
    implementation(Deps.appCompat)

    // Compose
    implementation(platform(Deps.composeBom))
    implementation(Deps.composeUiTooling)
    implementation(Deps.composeUiToolingPreview)
    implementation(Deps.composeUi)
    implementation(Deps.composeMaterial3)
    implementation(Deps.navigationCompose)
    implementation(Deps.animatedNavigation)
}