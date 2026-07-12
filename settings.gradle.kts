@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Weatherify"

include(
    ":app:androidApp",
    ":app:iosApp",
    ":analytics",
    ":common-ui",
    ":feature:auth",
    ":feature:language",
    ":feature:payment",
    ":feature:finder",
    ":feature:home",
    ":feature:settings",
    ":navigation",
    ":network",
    ":storage"
)
