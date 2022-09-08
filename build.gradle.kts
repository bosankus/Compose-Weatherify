// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        classpath(BuildPlugins.buildGradle)
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version Versions.buildGradle apply false
    id("com.android.library") version Versions.buildGradle apply false
    id("org.jetbrains.kotlin.android") version Versions.kotlin apply false
    id("com.google.dagger.hilt.android") version Versions.hilt apply false
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version Versions.secretPlugin apply false
    id("org.jlleitschuh.gradle.ktlint") version Versions.ktLintVersion apply false
    id("com.diffplug.spotless") version Versions.spotlessVersion apply false
    id("com.github.ben-manes.versions") version Versions.benManes
}

tasks.named<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask>("dependencyUpdates").configure {
    checkForGradleUpdate = true
    gradleReleaseChannel = com.github.benmanes.gradle.versions.updates.gradle.GradleReleaseChannel.RELEASE_CANDIDATE.id
    revision = "integration" // See available revisions
    outputFormatter = "plain" // xml and json available too
    outputDir = "build/dependencyUpdates"
    reportfileName = "dependency_update_report"
}