import org.gradle.api.artifacts.dsl.DependencyHandler

object KmmVersions {
    const val ktor = "2.3.5"
    const val kotlinxSerialization = "1.6.0"
    const val kotlinxCoroutines = "1.7.3"
    const val koin = "3.5.6"
    const val kotlinxDateTime = "0.4.1"
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
    
    // DateTime
    const val kotlinxDateTime = "org.jetbrains.kotlinx:kotlinx-datetime:${KmmVersions.kotlinxDateTime}"
}

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

fun DependencyHandler.addKmmAndroidDependencies() {
    implementation(KmmDeps.ktorAndroid)
}

fun DependencyHandler.addKmmIOSDependencies() {
    implementation(KmmDeps.ktorIOS)
}

private fun DependencyHandler.implementation(depName: String) {
    add("implementation", depName)
}