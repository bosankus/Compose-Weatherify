import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    id("com.android.library")
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
        iosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "feature_payment"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":network"))
            implementation(KmmDeps.koinCore)
            implementation(KmmDeps.kotlinxCoroutinesCore)
            implementation(KmmDeps.kotlinxDateTime)
            implementation(KmmDeps.kmpLifecycleViewModel)
        }

        androidMain.dependencies {
            implementation(KmmDeps.koinAndroid)
        }

        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting

        @Suppress("UNUSED_VARIABLE")
        val iosMain by creating {
            dependsOn(commonMain.get())
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
    }
}

android {
    namespace = "bose.ankush.payment"
    compileSdk = ConfigData.compileSdkVersion

    defaultConfig {
        minSdk = ConfigData.minSdkVersion
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
