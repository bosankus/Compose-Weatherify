# Implementation Plan - Adding Wear OS Support

This plan outlines the steps required to extend the existing KMM + CMP "Weatherify" project to support Wear OS. We will leverage the existing KMP modules for business logic (ViewModels, Network, Storage) while implementing a tailored UI for the watch using Wear OS Compose Material 3.

## User Review Required

> [!IMPORTANT]
> The Wear OS app will be a new Android application module (`:app:wearApp`). It will reuse the logic from the KMP modules but requires a separate UI implementation due to the unique form factor (small, often round screens).

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///Users/t0304iw/Desktop/androidplay/weatherify/Compose-Weatherify/gradle/libs.versions.toml)
- Add Wear OS Compose versions (Stable: 1.6.2).
- Add `androidx.wear.compose:compose-material3`, `androidx.wear.compose:compose-foundation`, and `androidx.wear.compose:compose-navigation3` libraries.

#### [MODIFY] [settings.gradle.kts](file:///Users/t0304iw/Desktop/androidplay/weatherify/Compose-Weatherify/settings.gradle.kts)
- Include the new `:app:wearApp` module.

### New Wear OS Module [NEW]

#### [NEW] [:app:wearApp:build.gradle.kts](file:///Users/t0304iw/Desktop/androidplay/weatherify/Compose-Weatherify/app/wearApp/build.gradle.kts)
- Configure the Android Application plugin for Wear OS.
- Add dependencies to Wear Compose libraries and existing KMP modules (`:feature:home`, `:feature:finder`, `:common-ui`, etc.).

#### [NEW] [AndroidManifest.xml](file:///Users/t0304iw/Desktop/androidplay/weatherify/Compose-Weatherify/app/wearApp/src/main/AndroidManifest.xml)
- Define the Wear OS application and its main activity.
- Add Wear OS specific features like `android.hardware.type.watch`.

#### [NEW] [WearMainActivity.kt](file:///Users/t0304iw/Desktop/androidplay/weatherify/Compose-Weatherify/app/wearApp/src/main/java/bose/ankush/weatherify/WearMainActivity.kt)
- Entry point for the Wear OS app.
- Initialize Koin and set the content using Wear Compose.

### UI Implementation

#### [NEW] [WearApp.kt](file:///Users/t0304iw/Desktop/androidplay/weatherify/Compose-Weatherify/app/wearApp/src/main/java/bose/ankush/weatherify/presentation/WearApp.kt)
- Define the top-level Wear OS Composable using `AppScaffold`.
- Implement navigation using `androidx.wear.compose.navigation3`.

#### [NEW] [WeatherScreen.kt](file:///Users/t0304iw/Desktop/androidplay/weatherify/Compose-Weatherify/app/wearApp/src/main/java/bose/ankush/weatherify/presentation/WeatherScreen.kt)
- A specialized weather screen for Wear OS using `ScreenScaffold` and `TransformingLazyColumn`.
- Reuses ViewModels and data from `:feature:home`.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:wearApp:assembleDebug` to ensure the module builds correctly.

### Manual Verification
- Deploy to a Wear OS emulator or physical watch device.
- Verify that the weather data is fetched and displayed correctly on the small screen.
- Test swipe-to-dismiss navigation.
