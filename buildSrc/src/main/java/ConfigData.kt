
// SDK levels (compileSdk/minSdk/targetSdk) live in gradle/libs.versions.toml — read them via
// libs.versions.compileSdk/minSdk/targetSdk in each module's build.gradle.kts instead.
object ConfigData {
    const val versionCode = 101
    const val versionName = "1.1"
    const val multiDexEnabled = true
}
