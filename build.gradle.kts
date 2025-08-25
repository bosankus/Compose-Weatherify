// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        classpath(BuildPlugins.buildGradle)
        classpath(BuildPlugins.kotlinGradlePlugin)
        classpath(BuildPlugins.googleServicePlugin)
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version Versions.buildGradle apply false
    id("com.android.library") version Versions.buildGradle apply false
    id("org.jetbrains.kotlin.android") version Versions.kotlin apply false
    id("org.jetbrains.kotlin.multiplatform") version Versions.kotlin apply false
    id("org.jetbrains.kotlin.plugin.serialization") version Versions.kotlin apply false
    id("com.google.dagger.hilt.android") version Versions.hilt apply false
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version Versions.secretPlugin apply false
    id("org.jlleitschuh.gradle.ktlint") version Versions.ktLintGradlePlugin apply false
    id("com.diffplug.spotless") version Versions.spotlessVersion apply false
    id("io.gitlab.arturbosch.detekt") version Versions.detekt apply false
    id("com.github.ben-manes.versions") version Versions.benManes
    id("org.jetbrains.kotlin.plugin.compose") version Versions.kotlin apply false
}

tasks.named<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask>("dependencyUpdates").configure {
    checkForGradleUpdate = true
    gradleReleaseChannel = com.github.benmanes.gradle.versions.updates.gradle.GradleReleaseChannel.RELEASE_CANDIDATE.id
    revision = "integration" // See available revisions
    outputFormatter = "plain" // xml and json available too
    outputDir = "build/dependencyUpdates"
    reportfileName = "dependency_update_report"
}

// Deep clean task: runs all module clean tasks, then removes build artefacts and repo-local .gradle
// Does NOT touch the user-level ~/.gradle cache.
tasks.register("deepClean") {
    description = "Cleans every module and removes all build artefacts in this repo."
    group = "build setup"

    // Run each subproject's own clean task first (honors plugin-specific clean hooks)
    dependsOn(subprojects.map { "${it.path}:clean" })

    doLast {
        val dirsToDelete = mutableSetOf<File>().apply {
            allprojects.forEach { add(it.layout.buildDirectory.get().asFile) }
            add(rootProject.layout.projectDirectory.dir(".gradle").asFile)
        }
        delete(dirsToDelete)
    }
}

// Spotless + ktlint configuration for all subprojects
subprojects {
    apply(plugin = "com.diffplug.spotless")

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            targetExclude("**/build/**")
            ktlint(Versions.ktLintCli).editorConfigOverride(
                mapOf(
                    "ktlint_code_style" to "ktlint_official",
                    "indent_size" to "4",
                    "max_line_length" to "120",
                    // Allow common Android/KMP patterns without false positives
                    "ktlint_function_naming_ignore_when_annotated_with" to "Composable"
                )
            )
            trimTrailingWhitespace()
            endWithNewline()
        }
        kotlinGradle {
            target("**/*.gradle.kts")
            ktlint(Versions.ktLintCli)
        }
    }
}

// Detekt minimal configuration for all subprojects
subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension>("detekt") {
        buildUponDefaultConfig = true
        allRules = false
        ignoreFailures = true
        autoCorrect = false
        parallel = true
    }

    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        jvmTarget = "21"
        reports {
            xml.required.set(false)
            txt.required.set(false)
            sarif.required.set(false)
            md.required.set(false)
            html.required.set(true)
        }
    }
}

// Aggregator tasks
tasks.register("spotlessCheckAll") {
    group = "verification"
    description = "Runs spotlessCheck in all subprojects"
    dependsOn(subprojects.map { "${it.path}:spotlessCheck" })
}

tasks.register("spotlessApplyAll") {
    group = "formatting"
    description = "Runs spotlessApply in all subprojects"
    dependsOn(subprojects.map { "${it.path}:spotlessApply" })
}

tasks.register("detektAll") {
    group = "verification"
    description = "Runs detekt in all subprojects"
    dependsOn(subprojects.map { "${it.path}:detekt" })
}

// Convenience task for minimal-risk code cleanup
// This applies formatting (imports/whitespace) only; safe to run locally
// Commit separately to avoid noisy diffs.
tasks.register("applyCodeCleanup") {
    group = "formatting"
    description = "Applies formatting across all subprojects (spotlessApplyAll)"
    dependsOn("spotlessApplyAll")
}
