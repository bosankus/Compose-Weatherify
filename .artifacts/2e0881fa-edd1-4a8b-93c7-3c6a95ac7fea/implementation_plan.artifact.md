# Implement Reducer in Home Module

This plan introduces a `HomeReducer` to the MVI architecture of the `home` module. Currently, `HomeViewModel` manages state transitions internally through direct `_state.update` calls. A Reducer will decouple state transition logic from side-effect orchestration (ViewModel), making the state machine easier to test and reason about.

## User Review Required

> [!IMPORTANT]
> This change introduces a `HomeAction` (Partial State) layer between the `HomeIntent` and `HomeState`. It slightly increases boilerplate but improves predictability of state transitions.

## Proposed Changes

### [Home Feature - Presentation Layer]

#### [NEW] [HomeAction](file:///Users/t0304iw/Desktop/androidplay/weatherify/Compose-Weatherify/feature/home/src/commonMain/kotlin/bose/ankush/home/presentation/HomeAction.kt)
```kotlin
package bose.ankush.home.presentation

import bose.ankush.home.domain.model.AirQuality
import bose.ankush.home.domain.model.WeatherForecast

internal sealed interface HomeAction {
    data class Loading(val isRefreshing: Boolean = false) : HomeAction
    data class FetchDataSuccess(
        val weather: WeatherForecast,
        val airQuality: AirQuality,
        val location: Pair<Double, Double>,
        val isOverridden: Boolean,
        val overrideName: String?,
    ) : HomeAction
    data class Error(val message: String, val isGpsDisabled: Boolean = false) : HomeAction
    data class UpdateNotificationBanner(val show: Boolean, val isPermanentlyDeclined: Boolean? = null) : HomeAction
    data class SetOffline(val isOffline: Boolean) : HomeAction
    data object DismissNotificationBanner : HomeAction
}
```

#### [NEW] [HomeReducer](file:///Users/t0304iw/Desktop/androidplay/weatherify/Compose-Weatherify/feature/home/src/commonMain/kotlin/bose/ankush/home/presentation/HomeReducer.kt)
```kotlin
package bose.ankush.home.presentation

internal object HomeReducer {
    fun reduce(state: HomeState, action: HomeAction): HomeState = when (action) {
        is HomeAction.Loading -> state.copy(
            isLoading = !action.isRefreshing,
            isRefreshing = action.isRefreshing,
            error = null
        )
        is HomeAction.FetchDataSuccess -> state.copy(
            isLoading = false,
            isRefreshing = false,
            weatherData = action.weather,
            airQualityData = action.airQuality,
            userLocation = action.location,
            isLocationOverridden = action.isOverridden,
            activeLocationName = action.overrideName,
            error = null
        )
        is HomeAction.Error -> state.copy(
            isLoading = false,
            isRefreshing = false,
            error = action.message,
            isGpsDisabled = action.isGpsDisabled
        )
        is HomeAction.UpdateNotificationBanner -> state.copy(
            showNotificationBanner = action.show,
            isNotificationPermissionPermanentlyDeclined = action.isPermanentlyDeclined ?: state.isNotificationPermissionPermanentlyDeclined
        )
        is HomeAction.SetOffline -> state.copy(isOffline = action.isOffline)
        HomeAction.DismissNotificationBanner -> state.copy(showNotificationBanner = false)
    }
}
```

#### [MODIFY] [HomeViewModel](file:///Users/t0304iw/Desktop/androidplay/weatherify/Compose-Weatherify/feature/home/src/commonMain/kotlin/bose/ankush/home/presentation/HomeViewModel.kt)
The ViewModel will now use `reduce` for all state updates. Example:
```kotlin
private fun dispatch(action: HomeAction) {
    _state.update { HomeReducer.reduce(it, action) }
}

// In processIntent or other methods:
// dispatch(HomeAction.Loading(isRefreshing = true))
```


## Verification Plan

### Automated Tests
- I will check if there are existing tests for `HomeViewModel` and update them or add new tests for the `HomeReducer`.

### Manual Verification
- Deploy the app and verify that the Home screen still functions correctly (Loading, Refreshing, Error handling, Weather data display).
