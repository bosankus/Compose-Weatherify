@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)

import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

// Prefix of the `swiftPMImport.<group>.<module>.*` namespace the SwiftPM cinterop bindings are
// generated under (see RazorpayCheckoutBridge.kt) — keep in sync with the imports there.
group = "bose.ankush"

// There's no iOS-native equivalent of the secrets-gradle-plugin androidApp uses to
// expose RAZORPAY_KEY via BuildConfig, so this mirrors it by hand: read the same
// secrets.properties (developer-local, gitignored) falling back to the committed
// secrets.defaults.properties placeholder, and emit a tiny generated Kotlin object.
// Runs eagerly at configuration time (cheap, single small file) so the generated
// source is present before the iosMain source set below is evaluated.
val generatedIosSecretsRoot =
    layout.buildDirectory
        .dir("generated/iosSecrets")
        .get()
        .asFile
run {
    val secretsFile =
        rootProject
            .file("secrets.properties")
            .takeIf { it.exists() }
            ?: rootProject.file("secrets.defaults.properties")
    val properties =
        Properties().apply {
            secretsFile.inputStream().use { load(it) }
        }
    val razorpayKey = properties.getProperty("RAZORPAY_KEY", "").trim('"')
    val packageDir = generatedIosSecretsRoot.resolve("bose/ankush/iosapp/generated")
    packageDir.mkdirs()
    packageDir.resolve("IosSecrets.kt").writeText(
        """
        package bose.ankush.iosapp.generated

        internal object IosSecrets {
            const val RAZORPAY_KEY: String = "$razorpayKey"
        }

        """.trimIndent(),
    )
}

kotlin {
    // Razorpay's iOS checkout SDK is cinterop'd here — the app-layer module, mirroring where the
    // Razorpay Android SDK is called from (MainActivity.kt), not inside the platform-agnostic
    // :feature:payment module. The Firebase packages the shared modules need are inherited from
    // :analytics and :feature:home; they must not be redeclared here or their Clang modules would
    // be generated twice.
    swiftPMDependencies {
        iosMinimumDeploymentTarget = "16.0"

        // The RazorpayCheckout product bundles four Clang modules, so auto-discovery is off and
        // only the two the bridge needs are imported:
        //
        // RazorpayStandard vends RazorpayCheckout itself and RazorpayCore the delegate protocols
        // (e.g. RazorpayPaymentCompletionProtocolWithData) — without RazorpayCore, protocol types
        // resolve to opaque, unimplementable `objcnames.protocols.*` placeholders and
        // RazorpayCheckout can't be constructed with a real delegate at all.
        //
        // RazorpayStandard is used rather than the (functionally identical) Razorpay module:
        // combining *Razorpay* + RazorpayCore in one cinterop pass hits a Kotlin/Native interop
        // generator bug under the Xcode 26.3 SDK ("'char8_tVar' is going to be declared twice") —
        // apparently triggered by Razorpay's extra WebKit-based initializers pulling in WKWebView
        // headers. RazorpayStandard + RazorpayCore avoids it while exposing the same API.
        discoverClangModulesImplicitly = false

        swiftPackage(
            url = url("https://github.com/razorpay/razorpay-pod.git"),
            version = from("1.5.4"),
            products = listOf(product("RazorpayCheckout")),
            importedClangModules = listOf("RazorpayStandard", "RazorpayCore"),
        )
    }

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
            dependsOn(commonMain.get())
            kotlin.srcDir(generatedIosSecretsRoot)
            dependencies {
                api(project(":common-ui"))
                api(project(":navigation"))
                api(project(":storage"))
                api(project(":network"))
                api(project(":analytics"))
                api(project(":feature:auth"))
                api(project(":feature:home"))
                api(project(":feature:finder"))
                api(project(":feature:settings"))
                api(project(":feature:language"))
                api(project(":feature:payment"))

                implementation(libs.compose.multiplatform.runtime)
                implementation(libs.compose.multiplatform.ui)
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
            }
        }

        getByName("iosArm64Main") {
            dependsOn(iosMain)
        }

        getByName("iosSimulatorArm64Main") {
            dependsOn(iosMain)
        }
    }
}
