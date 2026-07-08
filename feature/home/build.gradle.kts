import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    android {
        namespace = "bose.ankush.home"
        compileSdk =
            libs.versions.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }

        androidResources.enable = true
    }

    iosArm64()
    iosSimulatorArm64()

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        binaries.framework {
            baseName = "feature_home"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":network"))
            implementation(project(":storage"))
            implementation(project(":common-ui"))
            implementation(libs.compose.multiplatform.resources)
            implementation(libs.compose.multiplatform.runtime)
            implementation(libs.compose.multiplatform.foundation)
            implementation(libs.compose.multiplatform.material3)
            implementation(libs.compose.multiplatform.ui)
            implementation(libs.compose.multiplatform.ui.tooling.preview)
            implementation(libs.compose.multiplatform.animation)
            implementation(libs.compose.multiplatform.materialIconsExtended)
            implementation(libs.koin.core)
            implementation(libs.koin.core.viewmodel)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.androidx.lifecycle.viewmodel.kmp)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.coil3.compose)
            implementation(libs.coil3.network.ktor)
            implementation(libs.ktor.client.core)
        }

        androidMain.dependencies {
            implementation(libs.google.play.services.location)
            implementation(libs.koin.android)
            implementation(libs.firebase.config)
            implementation(libs.androidx.compose.ui.tooling)
            implementation(libs.androidx.compose.ui.tooling.preview)
        }

        val iosMain by creating {
            dependsOn(commonMain.get())
        }

        @Suppress("UNUSED_VARIABLE")
        val iosArm64Main by getting {
            dependsOn(iosMain)
        }

        @Suppress("UNUSED_VARIABLE")
        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }
    }
}

compose.resources {
    packageOfResClass = "bose.ankush.home.generated.resources"
}

dependencies {
    add("androidMainImplementation", platform(libs.firebase.bom))
}
