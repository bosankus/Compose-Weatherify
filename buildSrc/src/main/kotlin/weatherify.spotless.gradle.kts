plugins {
    id("com.diffplug.spotless")
}

// Single source of truth for the ktlint engine used by Spotless.
val ktlintVersion = "1.7.1"

spotless {
    kotlin {
        target("src/**/*.kt")
        targetExclude("**/build/**")
        ktlint(ktlintVersion).editorConfigOverride(
            mapOf(
                "ktlint_code_style" to "ktlint_official",
                "indent_size" to "4",
                "max_line_length" to "120",
                "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
                "ktlint_standard_package-name" to "disabled",
                "ktlint_standard_backing-property-naming" to "disabled"
            )
        )
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint(ktlintVersion)
    }
}
