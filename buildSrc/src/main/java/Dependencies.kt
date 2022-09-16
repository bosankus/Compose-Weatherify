// Plugins
object BuildPlugins {
    val buildGradle by lazy { "com.android.tools.build:gradle:${Versions.buildGradle}" }
}

// Dependencies
object Deps {
    // Unit Testing
    val junit by lazy { "junit:junit:${Versions.junit}" }
    val truth by lazy { "com.google.truth:truth:${Versions.truth}" }
    val turbine by lazy { "app.cash.turbine:turbine:${Versions.turbine}" }
    val coroutineTest by lazy { "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.coroutineTest}" }
    val coreTesting by lazy { "androidx.arch.core:core-testing:${Versions.coreTesting}" }
    val mockitoInline by lazy { "org.mockito:mockito-inline:${Versions.mockitoInline}" }
    val mockitoNhaarman by lazy { "com.nhaarman.mockitokotlin2:mockito-kotlin:${Versions.mockitoNhaarman}" }
    val mockWebServer by lazy { "com.squareup.okhttp3:mockwebserver:${Versions.mockWebServer}" }
    val mockk by lazy { "io.mockk:mockk:${Versions.mockk}" }

    // UI Testing
    val extJunit by lazy { "androidx.test.ext:junit:${Versions.extJunit}" }
    val espressoCore by lazy { "androidx.test.espresso:espresso-core:${Versions.espresso}" }
    val espressoContrib by lazy { "androidx.test.espresso:espresso-contrib:${Versions.espresso}" }
    val composeUiTest by lazy { "androidx.compose.ui:ui-test-junit4:${Versions.composeVersion}" }

    // Compose
    val composeUi by lazy { "androidx.compose.ui:ui:${Versions.composeVersion}" }
    val composeMaterial by lazy { "androidx.compose.material:material:${Versions.composeVersion}" }
    val composeUiTooling by lazy { "androidx.compose.ui:ui-tooling:${Versions.composeVersion}" }
    val composeUiToolingPreview by lazy { "androidx.compose.ui:ui-tooling-preview:${Versions.composeVersion}" }
    val composeMaterial3 by lazy { "androidx.compose.material3:material3:${Versions.composeMaterial3}" }

    // Core
    val androidCore by lazy { "androidx.core:core-ktx:${Versions.androidCore}" }
    val appCompat by lazy { "androidx.appcompat:appcompat:${Versions.appCompat}" }
    val androidMaterial by lazy { "com.google.android.material:material:${Versions.androidMaterial}" }
    val viewModelCompose by lazy { "androidx.lifecycle:lifecycle-viewmodel-compose:${Versions.lifecycle}" }
    val navigationCompose by lazy { "androidx.navigation:navigation-compose:${Versions.navigation}" }
    val inAppUpdate by lazy { "com.google.android.play:app-update:${Versions.googlePlayCore}" }
    val inAppUpdateKtx by lazy { "com.google.android.play:app-update-ktx:${Versions.googlePlayCore}" }
    val googlePlayLocation by lazy { "com.google.android.gms:play-services-location:${Versions.googlePlayLocation}" }
    val animatedNavigation by lazy { "com.google.accompanist:accompanist-navigation-animation:${Versions.accompanist}" }
    val composePermission by lazy { "com.google.accompanist:accompanist-permissions:${Versions.accompanist}" }

    // Networking
    val okHttp3 by lazy { "com.squareup.okhttp3:okhttp:${Versions.okHttp3}" }
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
    val hiltNavigationCompose by lazy { "androidx.hilt:hilt-navigation-compose:${Versions.hiltCompose}" }

    // Miscellaneous
    val timber by lazy { "com.jakewharton.timber:timber:${Versions.timber}" }
    val lottieCompose by lazy { "com.airbnb.android:lottie-compose:${Versions.lottie}" }
    val coilCompose by lazy { "io.coil-kt:coil-compose:${Versions.coilCompose}" }

    // Memory Leak
    val leakCanary by lazy { "com.squareup.leakcanary:leakcanary-android:${Versions.leakCanary}" }

    /*For Dialog module*/
    val composeRuntime by lazy { "androidx.compose.runtime:runtime:${Versions.composeVersion}" }
}