// Top-level build file where you can add configuration options common to all sub-projects/modules.
// Plugin versions all come from gradle/libs.versions.toml — the single source of truth for every
// dependency/plugin version across modules. No buildscript{} classpath block is needed: applying
// plugins below via the version catalog is enough to put them on every subproject's classpath.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.secrets.gradle.plugin) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ben.manes.versions)
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.google.services) apply false
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
            ktlint(libs.versions.ktlintCli.get()).editorConfigOverride(
                mapOf(
                    "ktlint_code_style" to "ktlint_official",
                    "indent_size" to "4",
                    "max_line_length" to "120",
                    // Allow common Android/KMP patterns without false positives
                    "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
                    // Project uses snake_case package segments (use_case, remote_config) — keep as-is
                    "ktlint_standard_package-name" to "disabled",
                    // Backing properties exposed via asStateFlow() functions rather than matching val — valid pattern
                    "ktlint_standard_backing-property-naming" to "disabled"
                )
            )
            trimTrailingWhitespace()
            endWithNewline()
        }
        kotlinGradle {
            target("**/*.gradle.kts")
            ktlint(libs.versions.ktlintCli.get())
        }
    }
}

// Detekt configuration for all subprojects
subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension>("detekt") {
        buildUponDefaultConfig = true
        allRules = false
        ignoreFailures = true
        autoCorrect = false
        parallel = true
        config.setFrom(files("$rootDir/config/detekt.yml"))
    }

    // Applies to both `detekt` and `detektAutoCorrect` tasks
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

    // Auto-correct variant — fixes the subset of rules detekt can patch automatically
    tasks.register("detektAutoCorrect", io.gitlab.arturbosch.detekt.Detekt::class.java) {
        description = "Runs detekt with auto-correct enabled"
        group = "verification"
        autoCorrect = true
        buildUponDefaultConfig = true
        config.setFrom(rootProject.files("config/detekt.yml"))
        ignoreFailures = true
        parallel = true
        setSource(files("src"))
        include("**/*.kt", "**/*.kts")
        exclude("**/build/**")
    }

    // Ensure detekt auto-correct runs after spotless has already formatted the files
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

tasks.named("detektAllAutoCorrect") { mustRunAfter("spotlessApplyAll") }
