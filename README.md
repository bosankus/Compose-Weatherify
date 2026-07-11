[![CI](https://github.com/bosankus/Compose-Weatherify/actions/workflows/ci.yml/badge.svg)](https://github.com/bosankus/Compose-Weatherify/actions/workflows/ci.yml)
[![Dependency Updates](https://github.com/bosankus/Compose-Weatherify/actions/workflows/check-dependency-updates.yml/badge.svg)](https://github.com/bosankus/Compose-Weatherify/actions/workflows/check-dependency-updates.yml)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/dda6430161e146518704730d9916dba7)](https://www.codacy.com/gh/bosankus/Compose-Weatherify/dashboard?utm_source=github.com&utm_medium=referral&utm_content=bosankus/Compose-Weatherify&utm_campaign=Badge_Grade)
![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-7F52FF?style=flat&logo=kotlin&logoColor=white)
![AGP](https://img.shields.io/badge/AGP-9.3.0--rc01-3DDC84?style=flat&logo=android&logoColor=white)
![Android](https://img.shields.io/badge/Min%20SDK-28%20(Pie)-3DDC84?style=flat&logo=android&logoColor=white)
![Version](https://img.shields.io/badge/Version-1.1-0078D4?style=flat)

# Weatherify

A production-grade Android weather app built with **Jetpack Compose**, **Clean Architecture** (MVI in the home and settings features), and a **Kotlin Multiplatform** module structure. It shows real-time weather, air quality data, saved locations, and sunrise/sunset animations — with multi-language support and an in-app premium upgrade flow.

[![Download APK](https://img.shields.io/badge/Download%20Latest%20APK-22272E.svg?style=for-the-badge&logo=android&logoColor=47954A)](https://github.com/bosankus/Compose-Weatherify/releases/latest)

---

## Features

| Category | Details |
|---|---|
| **Weather** | Current conditions, feels-like temp, humidity, wind speed (`:feature:home`) |
| **Air Quality** | Real-time AQI with pollutant details (`:feature:home`) |
| **Location** | GPS-based auto-detection with fallback, saved locations list, and place search (`:feature:finder`) |
| **Sunrise/Sunset** | Custom animated sunrise/sunset arc (`:common-ui` module) |
| **Multi-language** | English, Bengali (বাংলা), Hindi (हिन्दी), Kannada (ಕನ್ನಡ), Malayalam (മലയാളം), Tamil (தமிழ்), Telugu (తెలుగు), Hebrew (עברית) via Per-App Language API (`:feature:language` KMP module) |
| **Premium** | In-app purchase flow via Razorpay, surfaced as a bottom sheet inside Settings |
| **Notifications** | Firebase Cloud Messaging (FCM) push notifications |
| **In-App Updates** | Google Play in-app update prompts (`InAppUpdateManager`) |
| **Remote Config** | Server-driven feature flags gating the home experience (Firebase Remote Config, iOS + Android) |
| **Theming** | Material 3 + dynamic color + dark/light mode |

---

## Module Architecture

The project is split into clearly bounded Gradle modules. Everything except `:app` is a **Kotlin Multiplatform (KMP)** module (`org.jetbrains.kotlin.multiplatform` + `com.android.kotlin.multiplatform.library` plugins) with `commonMain`/`androidMain`/`iosMain` source sets — making the app iOS-portable without a full rewrite. `:app` stays a plain Android application module and is the only place Hilt is used; every KMP module uses **Koin**, bridged into `:app`'s Hilt graph via Koin-Hilt adapter modules. Navigation itself lives in its own KMP module, `:navigation`, which sits directly below `:app` and aggregates every feature module.

```mermaid
graph TD
    subgraph APP["🟦 :app  (Android, Hilt)"]
        A["WeatherifyApplication\nMainActivity\nAppPermissionViewModel"]
    end

    subgraph NAV["🟫 :navigation  (KMP, Koin)"]
        N["AppNavigation / AppNavHost - Nav3\nAppNavigator / rememberAppNavigationState\nRoutes / AppBottomBar\nplatform/ (permissions, back-press) actuals"]
    end

    subgraph HOME["🟨 :feature:home  (KMP, Koin, MVI)"]
        I["HomeFeatureRoute / HomeViewModel\nHomeReducer / HomeIntent / HomeAction / HomeEffect\nGetWeatherReport / GetAirQuality / RefreshWeatherReport\nWeatherRepositoryImpl / LocationClient / HomeGeocoder"]
    end

    subgraph SETTINGS["⚙️ :feature:settings  (KMP, Koin, MVI)"]
        J["SettingsFeatureRoute / SettingsViewModel (internal)\nSettingsReducer / SettingsIntent / SettingsAction\nOwn composeResources strings (8 locales)"]
    end

    subgraph COMMON["🟩 :common-ui  (KMP, Koin)"]
        B["InAppWebView\nServiceSubscriptionBottomSheet\nSunrise/Sunset Canvas Animation\nPermissionDialog\nDateFormatter"]
    end

    subgraph AUTH["🟦 :feature:auth  (KMP, Koin)"]
        H["LoginScreen\nAuthViewModel\nDeviceInfoProvider\ndomain-only, no data layer"]
    end

    subgraph PAYMENT["🟨 :feature:payment  (KMP, Koin)"]
        C["PaymentViewModel\nCreateOrderUseCase\nVerifyPaymentUseCase\nPremiumStore\nRazorpay checkout"]
    end

    subgraph NETWORK["🟧 :network  (KMP, Koin)"]
        D["Ktor Client\nWeatherApi\nKotlinx Serialization"]
    end

    subgraph STORAGE["🟥 :storage  (KMP, Koin)"]
        E["Room Database\nDataStore Preferences (incl. location prefs)\nWeatherDao"]
    end

    subgraph LANGUAGE["🟪 :feature:language  (KMP, Koin)"]
        F["LanguageScreen\nLocaleHelper"]
    end

    subgraph FINDER["🔍 :feature:finder (KMP, Koin)"]
        G["SavedLocationsScreen\nPlaceSearchDialog\nGetSavedLocationsUseCase\nFinderRepository / Impl"]
    end

    APP --> NAV
    APP --> HOME
    APP --> SETTINGS
    APP --> COMMON
    APP --> AUTH
    APP --> PAYMENT
    APP --> NETWORK
    APP --> STORAGE
    APP --> LANGUAGE
    APP --> FINDER
    NAV --> HOME
    NAV --> SETTINGS
    NAV --> COMMON
    NAV --> AUTH
    NAV --> PAYMENT
    NAV --> LANGUAGE
    NAV --> FINDER
    SETTINGS --> COMMON
    SETTINGS --> PAYMENT
    SETTINGS --> NETWORK
    HOME --> NETWORK
    HOME --> STORAGE
    HOME --> COMMON
    FINDER --> NETWORK
    FINDER --> PAYMENT
    FINDER --> COMMON
    PAYMENT --> NETWORK
    PAYMENT --> STORAGE
    AUTH --> NETWORK
    NETWORK --> STORAGE
```

> **DI note:** `:app` uses Hilt only for `AppPermissionViewModel`. Every KMP module (`:navigation`, `:feature:home`, `:feature:settings`, `:common-ui`, `:feature:*`, `:network`, `:storage`) uses Koin internally, registered in `WeatherifyApplication.initKoin()`. `app/.../di/NetworkModule.kt` and `di/StorageModule.kt` bridge Koin-registered network/storage dependencies into Hilt; `app/.../di/PaymentKoinModule.kt` is itself a **Koin** module (`appPaymentKoinModule`, despite the name) providing `PaymentConfig`.

---

## Clean Architecture

Each feature is structured across three layers, with dependency arrows pointing **inward** — the domain layer has zero Android or framework dependencies. `:feature:home`, `:feature:finder`, and `:feature:payment` follow this strictly with a full `domain/` + `data/` split (repository interfaces vs. impls, use cases, mappers). `:feature:home` and `:feature:settings` additionally follow an **MVI** pattern in their presentation layer (`Intent` → `Reducer` → `State`/`Effect`) rather than plain imperative state updates; in both, the `ViewModel`/`Reducer`/`Action`/`State` types are `internal` and only a single public `*FeatureRoute` composable is exposed. `:feature:auth` is lighter-weight: it only defines a `domain/DeviceInfoProvider` interface with a platform-specific implementation wired directly through Koin — there is no separate `data/` package for auth.

```mermaid
graph LR
    subgraph Presentation["🎨 Presentation Layer"]
        UI["Compose Screens\n(HomeFeatureRoute, SavedLocationsScreen\nSettingsFeatureRoute, LoginScreen, LanguageScreen)"]
        VM["ViewModels (internal)\n(HomeViewModel [MVI], PaymentViewModel\nAuthViewModel, SettingsViewModel [MVI])"]
        UI -- "UI Events / Intents" --> VM
        VM -- "UI State (StateFlow)" --> UI
    end

    subgraph Domain["🧠 Domain Layer"]
        UC["Use Cases\n(GetWeatherReport, GetAirQuality, RefreshWeatherReport\nGetSavedLocationsUseCase\nCreateOrderUseCase, VerifyPaymentUseCase...)"]
        REPO_IF["Repository Interfaces\n(WeatherRepository, FinderRepository, PaymentRepository...)"]
        UC --> REPO_IF
    end

    subgraph Data["💾 Data Layer"]
        REPO_IMPL["Repository Impls\n(WeatherRepositoryImpl, FinderRepositoryImpl\nPaymentRepositoryImpl)"]
        MAPPER["Mappers\n(Network → Storage\nStorage → Domain)"]
        REPO_IMPL --> MAPPER
    end

    subgraph External["🌐 External Sources"]
        NET[":network\nKtor + Androidplay Weather API"]
        DB[":storage\nRoom DB + DataStore"]
        PAY["Razorpay SDK"]
    end

    VM -- "calls" --> UC
    UC -- "calls" --> REPO_IF
    REPO_IF -. "implemented by" .-> REPO_IMPL
    REPO_IMPL --> NET
    REPO_IMPL --> DB
    REPO_IMPL --> PAY
```

---

## Navigation (Navigation 3)

Navigation lives in its own KMP module, **`:navigation`** (package `bose.ankush.navigation`), not inside `:app`. It runs on **Jetpack Navigation 3** (`androidx.navigation3`), not the older `NavHost`/`NavController` API. `AppNavigation.kt` builds an `entryProvider`, and `AppNavHost.kt` renders it through `NavDisplay` on Android (via an `expect`/`actual` — the Android `actual` in `AppNavHost.android.kt` uses `navigation3-ui` directly, since it has no iOS artifact yet; iOS gets a minimal custom renderer in `AppNavHost.ios.kt`). Backstack state is driven by a custom `AppNavigator` class / `rememberAppNavigationState()` wrapper, both in `AppNavigator.kt`. Routes are `@Serializable` `NavKey` objects defined in `Routes.kt`. The bottom bar (`AppBottomBar.kt`, also in `:navigation`) drives three top-level tabs: Home, Saved Locations, and Settings. `:app`'s `MainActivity` just calls `bose.ankush.navigation.AppNavigation(...)` inside its Hilt/Koin-composed `setContent`.

`:navigation`'s `platform/` package holds `expect`/`actual` wrappers for permission requests and back-press handling (`PlatformPermissions`, `NotificationPermissionRequest`, `ExitAppOnBackPress`), each with Android and iOS actuals.

```mermaid
graph TD
    Home["HomeRoute\n(HomeFeatureRoute — :feature:home)"] -->|bottom bar| Saved["SavedLocationsRoute\n(SavedLocationsFinderRoute — :feature:finder)"]
    Home -->|bottom bar| Settings["SettingsRoute\n(SettingsFeatureRoute — :feature:settings)"]
    Settings -->|language row, API 33+| Language["LanguageRoute(languages)\n(LanguageScreen — :feature:language)"]
    Settings -.->|API < 33| SystemSettings["System App Locale Settings"]
    Settings -->|premium bottom sheet| Payment["Razorpay checkout\n(PaymentViewModel, in-place sheet — no route)"]
```

Notes:
- There is no standalone `ProfileScreen` or `PaymentScreen` route — profile info and the premium upgrade flow are both embedded inside `SettingsFeatureRoute` (profile header + a premium bottom sheet), and `SettingsViewModel` is `internal` to `:feature:settings` (Koin-resolved), not exposed to `:app`.
- `InAppWebView` (`:common-ui`) is a composable (not a route) shown conditionally inside `SettingsFeatureRoute` for Terms/Privacy links.
- On Android 13+ (`isDeviceSDKAndroid13OrAbove()`), language selection pushes `LanguageRoute`; below API 33 it deep-links to the system per-app language settings instead.
- The old quick city switcher (`CitiesListRoute`) has been fully removed from the codebase — superseded by `:feature:finder`'s saved-locations flow.

---

## Data Flow

```text
Androidplay Weather API (https://data.androidplay.in)
       │  JSON (Ktor + Kotlinx Serialization)
       ▼
  :network module  ──────►  Network Models
                                  │
                             NetworkToStorageMapper
                                  │
                                  ▼
                         :storage module (Room DB / DataStore)
                                  │
                             Storage → Domain mapper
                                  │
                                  ▼
                            Domain Models
                                  │
                       Use Cases (:feature:home domain layer)
                                  │
                                  ▼
                    HomeViewModel — HomeIntent → HomeReducer → HomeState
                                  │
                                  ▼
                  Jetpack Compose UI (screens, via Nav3 NavDisplay)
```

---

## Tech Stack

### UI

| Library | Version | Purpose |
|---|---|---|
| Jetpack Compose BOM | `2026.06.00` | Declarative UI framework |
| Compose Multiplatform | `1.11.1` | Shared Compose UI for KMP modules |
| Material 3 | `1.9.0` | Design system + dynamic theming |
| Navigation 3 (`androidx.navigation3`) | `1.1.4` | Type-safe backstack + `NavDisplay`/`entryProvider` |
| Lifecycle ViewModel Nav3 | `2.11.0` | ViewModel scoping for Nav3 entries |
| Coil Compose | `2.7.0` (Android) / Coil3 `3.4.0` (KMP modules) | Async image loading |
| Splash Screen API | `1.2.0` | Android 12+ splash screen |

### Architecture & DI

| Library | Version | Purpose |
|---|---|---|
| Hilt | `2.59.2` | Dependency injection — `:app` module only (`AppPermissionViewModel`) |
| Koin | `4.2.2` | DI in all KMP modules (`:feature:home`, `:feature:settings`, `:common-ui`, `:feature:*`, `:network`, `:storage`) |
| Kotlin Coroutines | `1.11.0` | Async & structured concurrency |
| StateFlow / Flow | — | Reactive UI state management |

### Networking

| Library | Version | Purpose |
|---|---|---|
| Ktor Client | `3.5.1` | KMP-compatible HTTP client |
| Kotlinx Serialization | `1.11.0` | JSON parsing |
| OkHttp MockWebServer | `4.12.0` | Network mocking (declared; not yet exercised by tests — see Testing) |

### Local Storage

| Library | Version | Purpose |
|---|---|---|
| Room | `2.8.4` | SQLite ORM (weather cache) — requires 2.7+ to emit Kotlin under the KMP Android library plugin |
| DataStore Preferences | `1.2.1` | Key-value persistent settings, incl. location preferences |
| Kotlinx DateTime | `0.8.0` | KMP-compatible date/time |

### Firebase

| SDK | Purpose |
|---|---|
| Firebase BOM `34.15.0` | BoM for consistent versions |
| Analytics | Declared dependency; no explicit `logEvent` calls in code yet (auto-instrumentation only) |
| Remote Config | Server-driven feature flags (`HomeRemoteConfigGate`, Android + iOS implementations) |
| Performance Monitoring | Network + rendering metrics |
| Cloud Messaging (FCM) | Push notifications (`WeatherifyMessagingService`) |

### Testing

| Library | Purpose | Status |
|---|---|---|
| JUnit 4 + Truth | Unit assertions | Declared; **`app/src/test` does not exist — zero unit tests** |
| Turbine `1.2.1` | Flow/StateFlow testing | Declared, not yet exercised |
| Mockk `1.14.11` | Kotlin-first mocking | Declared, not yet exercised |
| Mockito + Nhaarman | Java-style mocking | Declared, not yet exercised |
| Espresso `3.7.0` + Hilt Testing | Instrumentation UI tests | Declared; **no `androidTest` sources exist anywhere**; `app/build.gradle.kts` still points `testInstrumentationRunner` at `bose.ankush.weatherify.helper.HiltTestRunner`, a class that doesn't exist in the repo |

> See [Testing Status](#testing-status) below — the test table above reflects declared dependencies, not actual coverage.

### Other

| Library | Purpose |
|---|---|
| Timber `5.0.1` | Structured logging |
| LeakCanary `2.14` | Memory leak detection (debug) |
| Razorpay `1.6.41` | In-app payment checkout |
| Google Play In-App Update | Forced/flexible update prompts (`InAppUpdateManager`) |
| Google Play Location `21.4.0` | FusedLocationProvider |

---

## Screens

```text
MainActivity (Hilt entry point)
└── AppNavigation (Nav3 NavDisplay + entryProvider — :navigation)
    ├── HomeFeatureRoute     — current weather + AQI card, MVI-driven               (:feature:home)
    ├── SavedLocationsFinderRoute — manage saved locations & search new places      (:feature:finder)
    ├── SettingsFeatureRoute — profile header, language row, notification toggle,   (:feature:settings)
    │                          premium bottom sheet, and in-app Terms/Privacy web view; MVI-driven
    │   └── InAppWebView     — in-app browser composable, not a separate route      (:common-ui)
    ├── LanguageScreen       — per-app language picker (Android 13+ only)           (:feature:language)
    └── LoginScreen          — authentication entry point                          (:feature:auth)
```

> There is no standalone `ProfileScreen` or `PaymentScreen` — both live inside `SettingsFeatureRoute`.

---

## Testing Status

Test infrastructure (Espresso, Hilt Testing, MockWebServer) is declared in the build files, but **there is currently no test coverage at all**:

- No `app/src/test` directory exists — the unit tests it previously held have been removed
- No `androidTest`/`androidInstrumentedTest` directory with content exists in any module (the placeholder `storage/src/androidInstrumentedTest/.../.gitkeep` is itself being deleted)
- No `commonTest`/`iosTest` source sets with test files exist in `:storage` or `:network`
- `app/build.gradle.kts` references a `HiltTestRunner` class as `testInstrumentationRunner` that doesn't exist anywhere in the repo — a dangling config left over from the removed tests

This is a known gap, not a design choice — treat the Testing table above as the target stack, not current coverage.

---

## Setup & Installation

### Prerequisites
- Android Studio Narwhal or later
- JDK 17 (JDK 21 is used by CI)

### Steps

1. **Clone the repo**
   ```bash
   git clone https://github.com/bosankus/Compose-Weatherify.git
   cd Compose-Weatherify
   ```

2. **Add `google-services.json`** to `app/` (from Firebase console — required for Analytics/FCM to compile).

3. **Build & run**
   ```bash
   ./gradlew assembleDebug
   # or just hit Run in Android Studio
   ```

> **Minimum Android version:** API 28 (Android 9 Pie)
> **Compile / Target SDK:** 37

---

## Code Quality

All style/lint versions and rules come from `gradle/libs.versions.toml` and `config/detekt.yml`, applied consistently across every subproject from the root `build.gradle.kts`.

| Task | What it does |
|---|---|
| `spotlessCheckAll` | Checks Kotlin/ktlint formatting across all modules (no changes made) |
| `detektAll` | Runs static analysis across all modules (no changes made) |
| `codeCheck` | `spotlessCheckAll` + `detektAll` — the full audit, matches CI's `lint` job |
| `spotlessApplyAll` | Auto-formats Kotlin/ktlint across all modules |
| `detektAllAutoCorrect` | Auto-corrects the subset of detekt rules that support it |
| `codeFormat` | `spotlessApplyAll` + `detektAllAutoCorrect` — the full auto-fix, matches CI's formatting step |

Before opening a PR, run everything in one shot:

```bash
./gradlew spotlessApplyAll detektAllAutoCorrect codeFormat
```

`codeFormat` already depends on `spotlessApplyAll` and `detektAllAutoCorrect`, so this single command applies formatting once and detekt auto-corrections once (Gradle de-duplicates the shared tasks) — then verify cleanly with `./gradlew codeCheck`.

---

## CI/CD

`.github/workflows/ci.yml` runs on every PR:

- **`build`** — JDK 21, Android SDK 36, `./gradlew build`, uploads the debug APK and publishes JUnit XML test results
- **`lint`** — runs `codeFormat` (auto-commits formatting fixes), then `codeCheck` (Spotless + Detekt)

`.github/workflows/check-dependency-updates.yml` runs weekly, executes the `dependencyUpdates` (ben-manes) task, and opens a GitHub issue if outdated dependencies are found.

---

## Contributing

Contributions are very welcome!

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Run `./gradlew spotlessApplyAll detektAllAutoCorrect codeFormat` before committing (see [Code Quality](#code-quality))
4. Commit using the project convention:
   ```text
   feat|fix|refactor|migrate|update: short description
   ```
5. Push and open a Pull Request against **`develop`**

CI builds the project and runs Spotless/Detekt checks on every PR (see [CI/CD](#cicd)).

---

## What's Next

These are the planned improvements currently in progress or on the roadmap:

- **iOS target** — the KMP foundation is in place across all non-`:app` modules (`commonMain`/`androidMain`/`iosMain`, including the new `:navigation` module which builds `iosArm64`/`iosSimulatorArm64` with a custom `AppNavHost` renderer since `navigation3-ui` has no iOS artifact yet). The next step is wiring up a SwiftUI host app and completing the remaining iOS-specific implementations.
- **Real test coverage** — the codebase currently has zero tests (see [Testing Status](#testing-status)): add `androidTest` instrumentation tests, exercise the already-declared Mockk/Turbine/MockWebServer dependencies, populate `commonTest`/`iosTest` source sets in `:storage`/`:network`, and fix or remove the dangling `HiltTestRunner` reference in `app/build.gradle.kts`.
- **Offline-first strategy** — full read-from-cache-then-network flow using Room as the single source of truth, with explicit stale-data indicators in the UI.
- **Widget support** — a Glance-based home screen widget showing current temperature and conditions.
- **Wear OS companion** — lightweight Wear Compose screen for wrist-based weather glances.
- **Release automation** — CI already builds and lints every PR; the next step is automated release builds and Play Store internal track deployments.
- **Accessibility pass** — semantic descriptions, touch target sizing, and TalkBack compatibility audit.

---

## License

This project intends to use the MIT License; a `LICENSE` file has not yet been added to the repository.
