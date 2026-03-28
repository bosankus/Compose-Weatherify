import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// NOTE: This module uses Compose Multiplatform (CMP) via direct Maven coordinates — no
// org.jetbrains.compose Gradle plugin needed. The Kotlin compose compiler is applied via
// org.jetbrains.kotlin.plugin.compose (declared in root build.gradle.kts).
// Before building, update CmpVersions.composeMultiplatform in buildSrc/KmmDeps.kt
// to the version compatible with your Kotlin version.

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "commonui"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(CmpDeps.runtime)
            implementation(CmpDeps.ui)
            implementation(CmpDeps.foundation)
            implementation(CmpDeps.material3)
            implementation(CmpDeps.animation)
            implementation(CmpDeps.components)
        }
        androidMain.dependencies {
            implementation(CmpDeps.uiTooling)
        }
    }
}

android {
    namespace = "bose.ankush.commonui"
    compileSdk = ConfigData.compileSdkVersion

    defaultConfig {
        minSdk = ConfigData.minSdkVersion
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
