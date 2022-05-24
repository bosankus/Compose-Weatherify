// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        classpath(BuildPlugins.buildGradle)
        classpath(BuildPlugins.hiltPlugin)
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id ("com.android.application") version Versions.buildGradle apply false
    id ("com.android.library") version Versions.buildGradle apply false
    id ("org.jetbrains.kotlin.android") version Versions.kotlin apply false
    id ("com.github.ben-manes.versions") version Versions.benManes
}