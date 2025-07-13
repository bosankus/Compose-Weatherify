plugins {
    kotlin("multiplatform")
    id("com.android.library")
    kotlin("plugin.serialization")
    id("kotlin-kapt")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = JavaVersion.VERSION_17.toString()
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "storage"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(KmmDeps.kotlinxCoroutinesCore)
                implementation(KmmDeps.koinCore)
                implementation(KmmDeps.kotlinxDateTime)
                implementation(KmmDeps.kotlinxSerialization)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                // Room dependencies
                implementation(Deps.room)
                implementation(Deps.roomKtx)
                // Gson for JSON serialization
                implementation("com.google.code.gson:gson:2.10.1")
                // Network module dependency
                implementation(project(":network"))
                // Dagger/Hilt dependencies
                implementation(Deps.hilt)
                // We can't use kapt here directly, it will be applied in the android block
            }
        }
        val androidUnitTest by getting
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
        val iosX64Test by getting
        val iosArm64Test by getting
        val iosSimulatorArm64Test by getting
        val iosTest by creating {
            dependsOn(commonTest)
            iosX64Test.dependsOn(this)
            iosArm64Test.dependsOn(this)
            iosSimulatorArm64Test.dependsOn(this)
        }
    }
}

android {
    namespace = "bose.ankush.storage"
    compileSdk = ConfigData.compileSdkVersion

    defaultConfig {
        minSdk = ConfigData.minSdkVersion
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Room schema location
    kapt {
        arguments {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }
}

// Apply kapt plugin for Room and Hilt annotation processing
dependencies {
    // Room annotation processor
    "kapt"(Deps.roomCompiler)
    // Hilt annotation processor
    "kapt"(Deps.hiltDaggerAndroidCompiler)
}
