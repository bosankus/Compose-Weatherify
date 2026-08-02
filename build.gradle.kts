// Top-level build file where you can add configuration options common to all sub-projects/modules.
// Plugin versions all come from gradle/libs.versions.toml — the single source of truth for every
// dependency/plugin version across modules. No buildscript{} classpath block is needed: applying
// plugins below via the version catalog is enough to put them on every subproject's classpath.
plugins {
    id("weatherify.spotless")
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.secrets.gradle.plugin) apply false
    alias(libs.plugins.ktlint) apply false
    // Declared here (not just via buildSrc) so it shares the same portal-resolved classloader as
    // kotlin.multiplatform — detekt's KMP task registration needs KotlinMultiplatformExtension
    // visible on its own classloader, which only holds if both plugins resolve from this one place.
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ben.manes.versions)
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.google.services) apply false
}

tasks.named<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask>("dependencyUpdates").configure {
    checkForGradleUpdate = true
    gradleReleaseChannel = com.github.benmanes.gradle.versions.updates.gradle.GradleReleaseChannel.RELEASE_CANDIDATE.id
    revision = "integration"
    outputFormatter = "plain"
    outputDir = "build/dependencyUpdates"
    reportfileName = "dependency_update_report"
}

// Deep clean task: runs all module clean tasks, then removes build artefacts and repo-local .gradle
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

// Quality checks configuration (spotless is a buildSrc convention plugin — see
// buildSrc/src/main/kotlin/weatherify.spotless.gradle.kts).
//
// Detekt stays configured here rather than as a convention plugin: its Kotlin-multiplatform task
// registration (DetektMultiplatform) references KotlinMultiplatformExtension directly, and that
// class is only visible on the classloader that resolved kotlin.multiplatform for this project —
// the one created by the `alias(libs.plugins.detekt) apply false` declaration above. Applying
// detekt from a buildSrc precompiled script instead loads it via buildSrc's own classloader, which
// can't see that per-project class and fails with NoClassDefFoundError.
allprojects {
    apply(plugin = "weatherify.spotless")
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension>("detekt") {
        buildUponDefaultConfig = true
        allRules = false
        ignoreFailures = true
        autoCorrect = false
        parallel = true
        config.setFrom(rootProject.layout.projectDirectory.file("config/detekt.yml"))
    }

    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        jvmTarget = "17"
        reports {
            xml.required.set(false)
            txt.required.set(false)
            sarif.required.set(false)
            md.required.set(false)
            html.required.set(true)
        }
    }

    tasks.register<io.gitlab.arturbosch.detekt.Detekt>("detektAutoCorrect") {
        description = "Runs detekt with auto-correct enabled"
        group = "verification"
        autoCorrect = true
        buildUponDefaultConfig = true
        config.setFrom(rootProject.layout.projectDirectory.file("config/detekt.yml"))
        ignoreFailures = true
        parallel = true
        setSource(files("src"))
        include("**/*.kt", "**/*.kts")
        exclude("**/build/**")
    }

    tasks.named("detektAutoCorrect") { mustRunAfter("spotlessApply") }
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

tasks.register("detektAllAutoCorrect") {
    group = "formatting"
    description = "Runs detekt with auto-correct in all subprojects"
    dependsOn(subprojects.map { "${it.path}:detektAutoCorrect" })
}

tasks.named("detektAllAutoCorrect") { mustRunAfter("spotlessApplyAll") }

// Single command: audit all style and lint issues without modifying files
tasks.register("codeCheck") {
    group = "verification"
    description = "Checks formatting (spotless) and runs detekt across all subprojects"
    dependsOn("spotlessCheckAll", "detektAll")
}

// Single command: apply all auto-fixable formatting and lint corrections
tasks.register("codeFormat") {
    group = "formatting"
    description = "Applies spotless formatting and detekt auto-corrections across all subprojects"
    dependsOn("spotlessApplyAll", "detektAllAutoCorrect")
}