plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

@Suppress("UnstableApiUsage")
android {
    namespace = "bose.ankush.payment"
    compileSdk = ConfigData.compileSdkVersion

    defaultConfig {
        minSdk = ConfigData.minSdkVersion
        targetSdk = ConfigData.targetSdkVersion

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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}

dependencies {

    // Core
    implementation(Deps.androidCore)
    implementation(Deps.appCompat)
    implementation(Deps.androidMaterial)
    implementation(Deps.constrainLayout)

    // Unit Testing
    testImplementation(Deps.junit)

    // UI Testing
    androidTestImplementation(Deps.extJunit)
    androidTestImplementation(Deps.espressoCore)

    // payment sdk
    implementation(Deps.razorPay)
}