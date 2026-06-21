[![Dependency Updates](https://github.com/bosankus/Compose-Weatherify/actions/workflows/check-dependecy-updates.yml/badge.svg)](https://github.com/bosankus/Compose-Weatherify/actions/workflows/check-dependecy-updates.yml)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/dda6430161e146518704730d9916dba7)](https://www.codacy.com/gh/bosankus/Compose-Weatherify/dashboard?utm_source=github.com&utm_medium=referral&utm_content=bosankus/Compose-Weatherify&utm_campaign=Badge_Grade)
[![Qodana](https://github.com/bosankus/Compose-Weatherify/actions/workflows/code_quality.yml/badge.svg)](https://github.com/bosankus/Compose-Weatherify/actions/workflows/code_quality.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21-7F52FF?style=flat&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Min%20SDK-26%20(Oreo)-3DDC84?style=flat&logo=android&logoColor=white)
![Version](https://img.shields.io/badge/Version-1.1-0078D4?style=flat)

# Weatherify

A production-grade Android weather app built with **Jetpack Compose**, **Clean Architecture**, and a **Kotlin Multiplatform-ready** module structure. It shows real-time weather, 5-day forecasts, air quality data, and sunrise/sunset animations — with multi-language support and an in-app premium upgrade flow.

[![Download APK](https://img.shields.io/badge/Download%20Latest%20APK-22272E.svg?style=for-the-badge&logo=android&logoColor=47954A)](https://github.com/bosankus/Compose-Weatherify/releases/latest)

---

## Features

| Category | Details |
|---|---|
| **Weather** | Current conditions, feels-like temp, humidity, wind speed |
| **Forecast** | 5-day weather forecast with hourly breakdown |
| **Air Quality** | Real-time AQI with pollutant details |
| **Location** | GPS-based auto-detection + manual city search |
| **Sunrise/Sunset** | Custom animated sunrise/sunset arc (`:sunriseui` module) |
| **Multi-language** | English, Bengali (বাংলা), Hindi (हिन्दी), Kannada (ಕನ್ನಡ), Malayalam (മലയാളം), Tamil (தமிழ்), Telugu (తెలుగు), Hebrew (עברית) via Per-App Language API |
| **Premium** | In-app purchase flow via Razorpay with a premium bottom sheet |
| **Notifications** | Firebase Cloud Messaging (FCM) push notifications |
| **In-App Updates** | Google Play in-app update prompts |
| **Theming** | Material 3 + dynamic color + dark/light mode |

---

## Module Architecture

The project is split into clearly bounded Gradle modules. `common-ui` and `feature-payment` are **Kotlin Multiplatform (KMP)** modules with `commonMain`, `androidMain`, and `iosMain` source sets — making the app iOS-portable without a full rewrite.

```mermaid
graph TD
    subgraph APP["🟦 :app  (Android)"]
        A[WeatherifyApplication\nMainActivity\nMainViewModel]
    end

    subgraph COMMON["🟩 :common-ui  (KMP)"]
        B[SettingsScreen\nLoginScreen\nInAppWebView\nPermissionDialog\nDateFormatter]
    end

    subgraph PAYMENT["🟨 :feature-payment  (KMP)"]
        C[PaymentViewModel\nCreateOrderUseCase\nVerifyPaymentUseCase\nPremiumStore]
    end

    subgraph NETWORK["🟧 :network  (Android)"]
        D[Ktor Client\nWeatherApi\nKotlinx Serialization]
    end

    subgraph STORAGE["🟥 :storage  (Android)"]
        E[Room Database\nDataStore Preferences\nWeatherDao]
    end

    subgraph LANGUAGE["🟪 :language  (Android)"]
        F[LanguageScreen\nLocale Config]
    end

    subgraph SUNRISE["⬛ :sunriseui  (Android)"]
        G[Sunrise/Sunset\nCanvas Animation]
    end

    APP --> COMMON
    APP --> PAYMENT
    APP --> NETWORK
    APP --> STORAGE
    APP --> LANGUAGE
    APP --> SUNRISE
```

---

## Clean Architecture

Each feature inside `:app` is structured across three layers. Dependency arrows always point **inward** — the domain layer has zero Android or framework dependencies.

```mermaid
graph LR
    subgraph Presentation["🎨 Presentation Layer"]
        UI["Compose Screens\n(HomeScreen, CitiesListScreen\nProfileScreen, PaymentScreen)"]
        VM["ViewModels\n(MainViewModel, CitiesViewModel)"]
        UI -- "UI Events" --> VM
        VM -- "UI State (StateFlow)" --> UI
    end

    subgraph Domain["🧠 Domain Layer"]
        UC["Use Cases\n(GetWeatherReports\nGetForecastReports\nGetAirQuality...)"]
        REPO_IF["Repository Interfaces"]
        UC --> REPO_IF
    end

    subgraph Data["💾 Data Layer"]
        REPO_IMPL["WeatherRepositoryImpl"]
        MAPPER["Mappers\n(Network → Storage\nStorage → Domain)"]
        REPO_IMPL --> MAPPER
    end

    subgraph External["🌐 External Sources"]
        NET[":network\nKtor + OpenWeatherMap API"]
        DB[":storage\nRoom DB + DataStore"]
    end

    VM -- "calls" --> UC
    UC -- "calls" --> REPO_IF
    REPO_IF -. "implemented by" .-> REPO_IMPL
    REPO_IMPL --> NET
    REPO_IMPL --> DB
```

---

## Data Flow

```text
OpenWeatherMap API
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
                           Use Cases (domain layer)
                                  │
                                  ▼
                          MainViewModel / CitiesViewModel
                          (StateFlow<UIState>)
                                  │
                                  ▼
                        Jetpack Compose UI (screens)
```

---

## Tech Stack

### UI

| Library | Version | Purpose |
|---|---|---|
| Jetpack Compose BOM | `2025.06.01` | Declarative UI framework |
| Material 3 | BOM-managed | Design system + dynamic theming |
| Compose Navigation | `2.7.7` | Type-safe screen navigation |
| Accompanist Permissions | `0.36.0` | Runtime permissions in Compose |
| Coil Compose | `2.7.0` | Async image loading |
| Splash Screen API | `1.2.0` | Android 12+ splash screen |

### Architecture & DI

| Library | Version | Purpose |
|---|---|---|
| Hilt | `2.58` | Dependency injection (Android) |
| Koin | — | DI bridge for KMP modules |
| Kotlin Coroutines | `1.10.2` | Async & structured concurrency |
| StateFlow / Flow | — | Reactive UI state management |

### Networking

| Library | Version | Purpose |
|---|---|---|
| Ktor Client | — | KMP-compatible HTTP client |
| Kotlinx Serialization | — | JSON parsing |
| OkHttp MockWebServer | `4.12.0` | Network mocking in tests |

### Local Storage

| Library | Version | Purpose |
|---|---|---|
| Room | `2.8.4` | SQLite ORM (weather cache) |
| DataStore Preferences | `1.1.1` | Key-value persistent settings |
| Kotlinx DateTime | `0.6.2` | KMP-compatible date/time |

### Firebase

| SDK | Purpose |
|---|---|
| Firebase BOM `34.10.0` | BoM for consistent versions |
| Analytics | User behaviour tracking |
| Remote Config | Server-driven feature flags |
| Performance Monitoring | Network + rendering metrics |
| Cloud Messaging (FCM) | Push notifications |

### Testing

| Library | Purpose |
|---|---|
| JUnit 4 + Truth | Unit assertions |
| Turbine `1.2.1` | Flow/StateFlow testing |
| Mockk `1.14.9` | Kotlin-first mocking |
| Mockito + Nhaarman | Java-style mocking |
| Espresso | Instrumentation UI tests |
| Hilt Testing | DI in Android tests |

### Other

| Library | Purpose |
|---|---|
| Timber `5.0.1` | Structured logging |
| LeakCanary `2.13` | Memory leak detection (debug) |
| Razorpay `1.6.41` | In-app payment checkout |
| Google Play In-App Update | Forced/flexible update prompts |
| Google Play Location `21.3.0` | FusedLocationProvider |

---

## Screens

```text
MainActivity
├── HomeScreen          — current weather + AQI card + hourly strip
├── CitiesListScreen    — search & manage saved cities
├── ProfileScreen       — user profile & settings shortcut
├── SettingsScreen      — language, theme, notification toggles
├── LoginScreen         — authentication entry point
├── PaymentScreen       — Razorpay premium upgrade flow
└── InAppWebView        — in-app browser for T&C / privacy policy
```

---

## Setup & Installation

### Prerequisites
- Android Studio Narwhal or later
- JDK 17
- An [OpenWeatherMap](https://openweathermap.org/api) API key (free tier works)

### Steps

1. **Clone the repo**
   ```bash
   git clone https://github.com/bosankus/Compose-Weatherify.git
   cd Compose-Weatherify
   ```

2. **Add your API key** to `local.properties` (create the file if it doesn't exist):
   ```properties
   OPEN_WEATHER_API_KEY=your_api_key_here
   ```

3. **Add `google-services.json`** to `app/` (from Firebase console — required for Analytics/FCM to compile).

4. **Build & run**
   ```bash
   ./gradlew assembleDebug
   # or just hit Run in Android Studio
   ```

> **Minimum Android version:** API 26 (Android 8.0 Oreo)  
> **Target SDK:** 36

---

## Contributing

Contributions are very welcome!

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit using the project convention:
   ```text
   feat|fix|refactor|migrate|update: short description
   ```
4. Push and open a Pull Request against **`develop`**

Please check the [PR template](.github/PULL_REQUEST_TEMPLATE.md) before submitting.

---

## What's Next

These are the planned improvements currently in progress or on the roadmap:

- **iOS target** — the KMP foundation is in place (`commonMain`/`iosMain` source sets exist in `:common-ui` and `:feature-payment`). The next step is wiring up a SwiftUI host app and completing the iOS-specific implementations.
- **Navigation v3 migration** — active migration branch (`migration/navigation-3`) moving from Navigation 2.x to the new type-safe Navigation 3 APIs with full back-stack support.
- **Offline-first strategy** — full read-from-cache-then-network flow using Room as the single source of truth, with explicit stale-data indicators in the UI.
- **Widget support** — a Glance-based home screen widget showing current temperature and conditions.
- **Wear OS companion** — lightweight Wear Compose screen for wrist-based weather glances.
- **CI/CD pipeline** — automated release builds and Play Store internal track deployments via GitHub Actions.
- **Accessibility pass** — semantic descriptions, touch target sizing, and TalkBack compatibility audit.

---

## License

This project is open-sourced under the [MIT License](LICENSE).
