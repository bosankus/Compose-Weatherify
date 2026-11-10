# KMP in practice, Part 6: Wear OS on a shared core

## How a phone and a watch talk, and where that fits in a KMP project

*Part 6 of KMP in practice. Missed the last one? Part 5 covers AGP 9, SwiftPM, SKIE and the build problems that took longer than the architecture:*

[card: Part 5]

Weatherify has a Wear OS app. It shows the current weather, the next few hours and any active alert, and it reuses the same shared core as the phone and iOS apps.

If you've never built for Wear OS, the hard part isn't the UI. It's understanding how the watch gets its data. So this part starts with a short primer on how a phone and a watch connect, then shows how we plugged that into a KMP project, and ends with the bugs it cost us.

## Wear OS in five minutes

### Two apps, one identity

A Wear OS app is a separate APK that runs on the watch. Your phone app and your watch app install separately, but Wear OS treats them as **one app** only when two things match exactly:

- the **applicationId** (package name), and
- the **signing key**.

If either differs, the two apps can't see each other. No error, no log line. They just never receive each other's data.

### How the devices connect

The user pairs the watch with the phone once, through a companion app (Pixel Watch, Galaxy Wearable or the Wear OS app). After that, Google Play services on both devices keeps a connection open: Bluetooth when they're close, Wi‑Fi or the cloud when they're not.

You never open a socket or manage Bluetooth yourself. You hand data to Play services, and it delivers it when it can.

[figure: How the watch gets its weather (light flow diagram)]

### The Data Layer: four tools

The API for this is called the **Wearable Data Layer**. It has four parts, and choosing the right one is most of the design work:

- **DataClient**: a small key-value store that both devices share. You write an item at a path like `/weather`, and Play services syncs it to the other side, now or whenever the devices reconnect. Use it for **state**: "this is the latest forecast".
- **MessageClient**: a one-way message to a device that's connected **right now**. If it isn't, the message is lost. Use it for **commands**: "refresh now", "open this on the phone".
- **ChannelClient**: a stream between devices. Use it for **files and large payloads**, like audio or images.
- **CapabilityClient / NodeClient**: tells you which connected devices exist and which of them have your app installed. Use it to **find who to talk to** before you send a message.

[figure: The Data Layer in four tools (infographic)]

A useful rule: if the watch should still have the data after a reconnect, it's a DataClient item. If it only matters this second, it's a message.

### Receiving data

The watch has two ways to hear about new data:

- **A listener added in code** (`dataClient.addListener`). It only works while your app is open.
- **A `WearableListenerService` declared in the manifest.** Play services wakes your app to deliver the data, even when it isn't running. The manifest entry says which paths it cares about.

Most apps need both: the service for background updates, and a read on launch for anything that arrived before.

### Standalone or phone-backed?

A modern watch can have its own Wi‑Fi or LTE and call your backend directly. Google recommends standalone apps where possible. The catch is sign-in: typing a password on a watch is painful, and moving tokens between devices adds a security surface.

We chose the simpler model: **the phone fetches and authenticates, and the watch only displays.** The watch never talks to our backend.

## Where it plugs into KMP

Wear OS only exists on Android, but the decision to sync belongs to shared code. The repository that fetches weather lives in `commonMain`, and it's the natural place to say "a new forecast arrived, push it to the watch".

So we used the same pattern as Part 2: a contract in shared code, an implementation per platform.

```kotlin
// commonMain
interface WeatherWearSync {
    suspend fun sync(locationName: String, forecast: WeatherForecast)
}
```

The shared repository calls it right after it saves a fresh forecast:

```kotlin
weatherStorage.saveWeatherData(weatherStorageData, airQualityStorageData)
weatherWearSync.sync(placeName ?: DEFAULT_LOCATION_NAME, forecast)
```

On Android, the implementation writes a DataClient item. On iOS, it does nothing:

```kotlin
// androidMain
internal class AndroidWeatherWearSync(context: Context) : WeatherWearSync {
    private val dataClient = Wearable.getDataClient(context)

    override suspend fun sync(locationName: String, forecast: WeatherForecast) {
        val request = PutDataMapRequest.create("/weather").apply {
            dataMap.putString("location_name", locationName)
            dataMap.putString("forecast_json", Json.encodeToString(forecast))
            dataMap.putLong("synced_at", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()

        runCatching { dataClient.putDataItem(request).await() }
    }
}

// iosMain
internal class NoopWeatherWearSync : WeatherWearSync {
    override suspend fun sync(locationName: String, forecast: WeatherForecast) = Unit
}
```

Koin binds the right one per platform, so the shared repository never knows a watch exists. It's also easy to test: a fake `WeatherWearSync` in `commonTest` lets us assert that a successful refresh syncs exactly once.

### One schema on both ends

The watch app depends on the same `:network` module, so it decodes the same `@Serializable WeatherForecast` the phone encoded. There's no separate watch model to keep in sync. If the forecast shape changes, both sides change in the same commit.

### What about Apple Watch?

We haven't built one, but the contract is ready for it. Apple's WatchConnectivity has the same ideas under different names:

- `updateApplicationContext` is close to DataClient: the latest state, delivered when possible.
- `sendMessage` is close to MessageClient: only works while the other side is reachable.
- `transferFile` is close to ChannelClient.

An iOS implementation of `WeatherWearSync` would call `updateApplicationContext`, and nothing in shared code would change.

## Things that cost us time

### 1. The `.wear` applicationId

Our watch app started with `applicationId = "bose.ankush.weatherify.wear"`. It looked tidy. Sync never worked, and nothing said why. We spent time blaming the emulator image before finding the rule: the Data Layer only connects apps with the same package name and signing key.

```kotlin
defaultConfig {
    // Must match the phone app exactly, or the Data Layer never delivers.
    applicationId = "bose.ankush.weatherify"
}
```

The namespace can still be different (ours is `bose.ankush.weatherify.wear`). Only the applicationId has to match.

### 2. The path has to match in three places

The phone writes to `/weather`. The watch parser reads `/weather`. But the manifest registered our listener service for `/weather-forecast`:

```xml
<!-- before -->
<data android:scheme="wear" android:host="*" android:pathPrefix="/weather-forecast" />
<!-- after -->
<data android:scheme="wear" android:host="*" android:pathPrefix="/weather" />
```

`/weather` doesn't start with `/weather-forecast`, so Play services never woke the service. The app still looked fine, because it reads the last item on launch. What we'd lost was background updates: a refresh on the phone didn't reach a watch that wasn't open.

The fix is one line. The lesson is to keep the path in one constant, and to test the watch with the app closed.

### 3. Identical items don't fire

DataClient skips an item that's byte-for-byte the same as the last one. If the weather hasn't changed, the watch hears nothing. Adding a `synced_at` timestamp makes every sync a new item.

### 4. Listeners don't replay

The listener service only hears about changes that happen after it's registered. If the phone synced before the watch app was installed or started, that event is gone. So the watch reads whatever DataClient already holds when it launches:

```kotlin
val items = Wearable.getDataClient(this).dataItems.await()
items.mapNotNull { WeatherSyncStore.parse(it) }.forEach { WeatherSyncStore.update(it) }
items.release()
```

### 5. A watch is its own platform

- **minSdk 30.** Wear OS 3 and later need API 30. Our phone app supports 28, so the watch sets its own floor.
- **A different Compose library.** Wear uses `androidx.wear.compose.material3`, not phone Material 3. Components look similar but aren't interchangeable.
- **Swipe to go back.** Navigation 3 works on the watch, with a swipe-to-dismiss scene strategy instead of a back button.

## How to check you're doing it right

- The phone and watch apps have the same applicationId and are signed with the same key, in debug and release.
- Every Data Layer path lives in one constant that both sides and the manifest agree on.
- Close the watch app, refresh on the phone, and check that the watch's listener logs the update.
- Fresh install the watch app after the phone has synced, and check it shows data on first launch.

## The one thing to take away

> **The watch is easy to share code with and easy to disconnect from.** Put the sync decision in shared code behind a small contract, then treat the Data Layer's rules (same applicationId, same key, same path) as part of your build, not your UI.

## Where did I go wrong?

The phone-backed model was a deliberate trade-off: no sign-in on the watch, but no fresh weather without the phone nearby. If you've shipped a standalone Wear OS app with a sign-in flow people actually like, or you keep phone and watch models in sync a cleaner way, tell me in the comments. And if you've built the Apple Watch side of a KMP app, I'd like to see how you wired it.

*Next in the series: Automotive, and what you have to cut for a screen people look at while driving. New here? Start with Part 1:*

[card: Part 1]

*The overview, with every decision in the series in one place:*

[card: Overview]
