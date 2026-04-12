import org.gradle.api.artifacts.dsl.DependencyHandler

// Compose Multiplatform runtime dependencies for the common-ui KMP module.
// The version MUST match your Kotlin version. Check the table at:
// https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-compatibility-and-versioning.html
// Known mapping: Kotlin 2.1.x → CMP 1.7.x  |  Update below for your exact Kotlin version.
object CmpVersions {
    const val composeMultiplatform = "1.7.3"
}

object CmpDeps {
    const val runtime = "org.jetbrains.compose.runtime:runtime:${CmpVersions.composeMultiplatform}"
    const val ui = "org.jetbrains.compose.ui:ui:${CmpVersions.composeMultiplatform}"
    const val foundation =
        "org.jetbrains.compose.foundation:foundation:${CmpVersions.composeMultiplatform}"
    const val material3 =
        "org.jetbrains.compose.material3:material3:${CmpVersions.composeMultiplatform}"
    const val animation =
        "org.jetbrains.compose.animation:animation:${CmpVersions.composeMultiplatform}"
    const val components =
        "org.jetbrains.compose.components:components-resources:${CmpVersions.composeMultiplatform}"
    const val uiTooling = "org.jetbrains.compose.ui:ui-tooling:${CmpVersions.composeMultiplatform}"
}

object KmmVersions {
    const val ktor = "2.3.13"
    const val kotlinxSerialization = "1.7.3"
    const val kotlinxCoroutines = "1.9.0"
    const val koin = "3.5.6"
    const val koinAndroidCompose = "3.5.6"
    const val kotlinxDateTime = "0.6.1"
    const val kmpLifecycleViewModel = "2.8.4"
}

object KmmDeps {
    // Ktor
    const val ktorCore = "io.ktor:ktor-client-core:${KmmVersions.ktor}"
    const val ktorSerialization = "io.ktor:ktor-client-serialization:${KmmVersions.ktor}"
    const val ktorContentNegotiation = "io.ktor:ktor-client-content-negotiation:${KmmVersions.ktor}"
    const val ktorJson = "io.ktor:ktor-serialization-kotlinx-json:${KmmVersions.ktor}"
    const val ktorLogging = "io.ktor:ktor-client-logging:${KmmVersions.ktor}"

    // Platform-specific Ktor engines
    const val ktorAndroid = "io.ktor:ktor-client-android:${KmmVersions.ktor}"
    const val ktorIOS = "io.ktor:ktor-client-darwin:${KmmVersions.ktor}"

    // Kotlinx Serialization
    const val kotlinxSerialization = "org.jetbrains.kotlinx:kotlinx-serialization-json:${KmmVersions.kotlinxSerialization}"

    // Kotlinx Coroutines
    const val kotlinxCoroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${KmmVersions.kotlinxCoroutines}"

    // Koin
    const val koinCore = "io.insert-koin:koin-core:${KmmVersions.koin}"
    const val koinAndroid = "io.insert-koin:koin-android:${KmmVersions.koin}"
    const val koinAndroidCompose = "io.insert-koin:koin-androidx-compose:${KmmVersions.koinAndroidCompose}"

    // DateTime
    const val kotlinxDateTime = "org.jetbrains.kotlinx:kotlinx-datetime:${KmmVersions.kotlinxDateTime}"

    // KMP-compatible ViewModel (JetBrains port of AndroidX lifecycle-viewmodel)
    const val kmpLifecycleViewModel = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel:${KmmVersions.kmpLifecycleViewModel}"
}

@Suppress("unused")
fun DependencyHandler.addKmmCommonDependencies() {
    implementation(KmmDeps.ktorCore)
    implementation(KmmDeps.ktorSerialization)
    implementation(KmmDeps.ktorContentNegotiation)
    implementation(KmmDeps.ktorJson)
    implementation(KmmDeps.ktorLogging)
    implementation(KmmDeps.kotlinxSerialization)
    implementation(KmmDeps.kotlinxCoroutinesCore)
    implementation(KmmDeps.koinCore)
    implementation(KmmDeps.kotlinxDateTime)
}

@Suppress("unused")
fun DependencyHandler.addKmmAndroidDependencies() {
    implementation(KmmDeps.ktorAndroid)
}

@Suppress("unused")
fun DependencyHandler.addKmmIOSDependencies() {
    implementation(KmmDeps.ktorIOS)
}

private fun DependencyHandler.implementation(depName: String) {
    add("implementation", depName)
}