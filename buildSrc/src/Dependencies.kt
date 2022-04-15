// Plugins

object BuildPlugins {
    val buildGradle by lazy { "com.android.tools.build:gradle:${Versions.buildGradle}" }
    val navigationSafeArgs by lazy { "androidx.navigation:navigation-safe-args-gradle-plugin:${Versions.buildGradle}" }
    val hiltPlugin by lazy { "com.google.dagger:hilt-android-gradle-plugin:${Versions.hilt}" }
}


object Deps {
    // TODO
}