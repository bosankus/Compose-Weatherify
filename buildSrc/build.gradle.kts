plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation(libs.spotless.plugin.gradle)
}

kotlin {
    jvmToolchain(17)
}
