// Plugins
object BuildPlugins {
    val buildGradle by lazy { "com.android.tools.build:gradle:${Versions.buildGradle}" }
    val navigationSafeArgs by lazy { "androidx.navigation:navigation-safe-args-gradle-plugin:${Versions.buildGradle}" }
    val hiltPlugin by lazy { "com.google.dagger:hilt-android-gradle-plugin:${Versions.hilt}" }
}

// Dependencies
object Deps {
    // Testing
    val junit by lazy { "junit:junit:4.13.2:${Versions.junit}" }
    val extJunit by lazy { "androidx.test.ext:junit:1.1.3:${Versions.extJunit}" }
    val truth by lazy { "com.google.truth:truth:${Versions.truth}" }
    val turbine by lazy { "app.cash.turbine:turbine:0.7.0:${Versions.turbine}" }
    val coroutineTest by lazy { "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.coroutineTest}" }
    val coreTesting by lazy { "androidx.arch.core:core-testing:${Versions.coreTesting}" }
    val espressoCore by lazy { "androidx.test.espresso:espresso-core:${Versions.espressoCore}" }
    val mockitoInline by lazy { "org.mockito:mockito-inline:${Versions.mockitoInline}" }
    val mockitoNhaarman by lazy { "com.nhaarman.mockitokotlin2:mockito-kotlin:${Versions.mockitoNhaarman}" }

    // Core
    val androidCore by lazy { "androidx.core:core-ktx:${Versions.androidCore}" }
    val appCompat by lazy { "androidx.appcompat:appcompat:${Versions.appCompat}" }
    val multiDex by lazy { "com.android.support:multidex:${Versions.multiDex}" }
    val androidMaterial by lazy { "com.google.android.material:material:${Versions.androidMaterial}" }
    val constraintLayout by lazy { "androidx.constraintlayout:constraintlayout:${Versions.constraintLayout}" }
    val recyclerView by lazy { "androidx.recyclerview:recyclerview:${Versions.recyclerView}" }
    val fragment by lazy { "androidx.fragment:fragment-ktx:${Versions.fragment}" }
    val viewModel by lazy { "androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.lifecycle}" }
    val navigationFragment by lazy { "android.arch.navigation:navigation-fragment-ktx:${Versions.navigation}" }
    val navigationUi by lazy { "android.arch.navigation:navigation-ui-ktx:${Versions.navigation}" }

    // Networking
    val retrofit by lazy { "com.squareup.retrofit2:retrofit:${Versions.retrofit}" }
    val retrofitGson by lazy { "com.squareup.retrofit2:converter-gson:${Versions.retrofit}" }
    val retrofitCoroutineAdapter by lazy { "com.jakewharton.retrofit:retrofit2-kotlin-coroutines-adapter:${Versions.retrofitCoroutineAdapter}" }
    val okhttpInterceptor by lazy { "com.squareup.okhttp3:logging-interceptor:${Versions.okhttpInterceptor}" }

    // Coroutines
    val coroutinesCore by lazy { "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}" }
    val coroutinesAndroid by lazy { "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutines}" }

    // Dependency Injection
    val hilt by lazy { "com.google.dagger:hilt-android:${Versions.hilt}" }
    val hiltDaggerAndroidCompiler by lazy { "com.google.dagger:hilt-android-compiler:${Versions.hilt}" }
    val hiltAndroidxCompiler by lazy { "androidx.hilt:hilt-compiler:${Versions.hiltAndroidxCompiler}" }

    // Miscellaneous
    val timber by lazy { "com.jakewharton.timber:timber:${Versions.timber}" }
    val lottie by lazy { "com.airbnb.android:lottie:${Versions.lottie}" }
    val glide by lazy { "com.github.bumptech.glide:glide:${Versions.glide}" }

}