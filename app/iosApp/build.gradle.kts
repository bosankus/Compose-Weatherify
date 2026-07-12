plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    // iosX64 (Intel simulator) dropped: matches the rest of the shared modules —
    // Compose Multiplatform stopped publishing artifacts for it starting at 1.11.0.
    iosArm64()
    iosSimulatorArm64()

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        binaries.framework {
            // Named distinctly from the Xcode app target (also "iosApp") — otherwise
            // Swift's `import iosApp` self-resolves to the app module and can't see
            // this framework's symbols.
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        val iosMain by creating {
            dependencies {
                api(project(":common-ui"))
                api(project(":navigation"))
                api(project(":storage"))
                api(project(":network"))
                api(project(":feature:auth"))
                api(project(":feature:language"))
                api(project(":feature:payment"))
                api(project(":feature:finder"))
                api(project(":feature:home"))
                api(project(":feature:settings"))

                implementation(libs.compose.multiplatform.runtime)
                implementation(libs.compose.multiplatform.ui)
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
            }
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
