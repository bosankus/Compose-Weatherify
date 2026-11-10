# Taking a 4-year-old Android app to Kotlin Multiplatform: the architecture calls that mattered

*Weatherify, from one Android module to a shared core serving Android, iOS, Wear OS and Automotive.*

---

Weatherify started in 2021 as an ordinary Android app: Hilt, Retrofit, Room, MVVM, one `app` module. Today the same repository builds an Android phone app, a SwiftUI iOS app, a Wear OS app and an Android Automotive app. All of them run on one Kotlin core.

Some numbers to set the scale:

| Source set | Lines of Kotlin |
|---|---|
| `commonMain` (all modules) | ~20,000 |
| `androidMain` | ~9,900 (mostly Compose UI) |
| `iosMain` | ~5,100 (platform adapters and SwiftUI controllers) |
| `commonTest` | ~6,200 |
| `:app:androidApp` | **~1,300** |

The last row is the point of this post. The Android application module used to hold everything. Now it holds about 1,300 lines: `Application`, `MainActivity`, FCM, in-app update, and Koin startup. Everything else moved into modules that don't know which platform they run on.

This isn't a "what is KMP" post. It covers the decisions we made, the order we made them in, and the problems that cost us real time. I've tried to write each section so a senior Android engineer can take the idea to their own codebase even if they never touch iOS.

---

## 1. Migrate bottom-up, starting with the layers that have no UI

The migration did not start with screens. It went in this order:

1. **`:network`** (June 2025): Retrofit replaced by Ktor, with an Android engine and a Darwin engine.
2. **`:storage`** (July 2025): Room entities and DAOs moved behind a `WeatherStorage` interface.
3. **DI to Koin** (June 2026, then finished September 2026)
4. **Feature modules** (July 2026): auth, payment, language, finder, home, settings, one PR each.
5. **`:navigation`** and **`:routing`** (July 2026)
6. **iOS shell** (July to October 2026): first Compose Multiplatform screens, then native SwiftUI.

**Why this order works:** a data module has no UI and has clear inputs and outputs. You can check a migrated `:network` by running the Android app unchanged, because Android calls the same functions it always did, now backed by Ktor. Each step shipped on its own and the app never had a "half-migrated" week.

The reverse order (shared UI first) feels faster because something shows up on iOS straight away. But every screen you move pulls its whole dependency tree with it, and you end up migrating networking and storage under deadline pressure.

---

## 2. The module graph, and the rules that keep it a graph

```
                ┌──────────────┐   ┌──────────────┐
                │ androidApp   │   │ iosApp (SKIE)│   wearApp, automotive
                └──────┬───────┘   └──────┬───────┘
                       │                  │
                ┌──────▼──────────────────▼───────┐
                │           :navigation           │  Nav3 host (expect/actual)
                └──────┬──────────────────────────┘
                       │
   ┌──────────┬────────┼──────────┬───────────┬────────────┐
   ▼          ▼        ▼          ▼           ▼            ▼
 :feature  :feature :feature   :feature    :feature     :feature
  :home    :settings  :auth    :payment    :language    :finder
   │          │         │          │                       │
   └────┬─────┴────┬────┴──────────┴───────────┬───────────┘
        ▼          ▼                           ▼
   :network    :storage ──api──► :token-storage-api ◄── :network
        │          │
        └────┬─────┘
             ▼
   :analytics   :common-ui   :routing
```

Three rules hold this together:

- **Core modules (`network`, `storage`, `analytics`, `routing`, `token-storage-api`) never depend on features.**
- **Features expose very little.** `:feature:settings` exposes one public composable (`SettingsFeatureRoute`) and its Koin modules. `HomeViewModel` and `SettingsViewModel` are `internal`. The app module can't reach into a feature's state, so it can't depend on it by accident.
- **Apps only assemble.** They choose Koin modules, provide platform config (API keys, the Razorpay checkout), and host the root UI.

We haven't kept the rules perfectly. `:feature:home` depends on `:feature:finder` (saved places), and `:feature:settings` depends on `:feature:payment`. I come back to this at the end.

---

## 3. Put contracts in their own modules: `:token-storage-api`

This is the smallest module in the repo and the change I'd most like other teams to copy.

`:network` needs to read and write auth tokens. Tokens are stored in `:storage`, in EncryptedSharedPreferences on Android and the Keychain on iOS. So `:network` depended on `:storage`, which brought in Room, KSP, SQLite drivers and every entity, all to call four methods.

The fix was a module that contains nothing but interfaces:

```kotlin
// :token-storage-api — commonMain, no dependencies besides coroutines
interface TokenStorage {
    suspend fun saveTokens(accessToken: String, refreshToken: String?, tokenType: String?)
    suspend fun getToken(): String?
    suspend fun getRefreshToken(): String?
    suspend fun getTokenType(): String?
    fun hasToken(): Flow<Boolean>
    suspend fun clearToken()
}
```

`:network` uses `implementation(project(":token-storage-api"))`. `:storage` uses `api(project(":token-storage-api"))` and provides the implementations. Koin connects the two at the app level.

The same module also holds `DpopKey`, the hardware-backed signing key used for DPoP-bound tokens ([RFC 9449](https://www.rfc-editor.org/rfc/rfc9449)):

```kotlin
interface DpopKey {
    suspend fun publicKey(): EcPublicKey
    suspend fun sign(data: ByteArray): ByteArray   // ECDSA P-256, DER
    suspend fun delete()
}
```

`:storage` implements it twice: `KeystoreDpopKey` (androidMain) and `SecureEnclaveDpopKey` (iosMain). `:network` builds DPoP proofs in `commonMain` without knowing which secure hardware is underneath, and its tests use a `FakeDpopKey`.

> **Takeaway for Android teams:** this is the familiar `api`/`impl` split, and it matters more in KMP. Every unnecessary edge in the graph is another target that must compile for `iosArm64` and `iosSimulatorArm64`. Removing an edge saves build time on two platforms.

---

## 4. Dependency injection: why we left Hilt

We liked Hilt and didn't want to drop it. It turned out not to fit two constraints:

1. **Hilt is JVM-only.** Code generated by Dagger can't run in `commonMain`.
2. **Hilt can't aggregate `@Module`s from a `com.android.kotlin.multiplatform.library`.** When `:storage` moved to the AGP 9 KMP plugin, its Hilt modules stopped being discovered. The short-term fix was to move storage's Hilt wiring into the app module, which broke the module boundary we were trying to build.

So we moved to Koin in two stages:

- **Bridge stage (June 2026):** `network` and `storage` provided Koin modules. The app kept Hilt and used small bridge providers that fetched singletons from Koin. Screens didn't change.
- **Removal stage (September 2026, #224):** with every ViewModel resolved by Koin, the bridges, `@HiltAndroidApp`, `@AndroidEntryPoint`, and the Hilt and KSP dependencies were deleted from `:app:androidApp`. That PR removed 118 lines and added 35.

Every feature now has the same three-part DI layout:

```kotlin
// commonMain
expect val homePlatformModule: Module            // LocationClient, geocoder, Remote Config, on-device AI…

val homeDomainModule = module {
    single<WeatherRepository> { WeatherRepositoryImpl(get(), get(), get(), get()) }
    factory { ObserveHomeWeather(get(), get()) }
    // …
}

val homeViewModelModule = module {
    // Not viewModelOf: it would try to resolve the defaulted errorMessage parameter too.
    viewModel { HomeViewModel(get(), get(), get(), get(), get()) }
    viewModelOf(::NearbyViewModel)
}
```

`androidMain` binds `DeviceLocationClient` and `MlKitOnDeviceAiClient`. `iosMain` binds `IosLocationClient` and an AI client whose implementation comes from Swift. Each app's `startKoin {}` lists the modules it wants.

**What we lost:** compile-time graph validation. Under Hilt, a missing binding failed the build. Under Koin, it fails when the dependency is first resolved. The comment in the snippet above shows one way this bites: `viewModelOf(::X)` tries to resolve *every* constructor parameter, including ones with default values. If you migrate, add a Koin `verify()` / `checkModules` test per feature to recover some of that safety.

---

## 5. Persistence: Room KMP and the missing `_Impl`

Room has supported KMP since 2.7. Moving to it surfaced the most confusing crash of the migration:

```
RuntimeException: Cannot find implementation for bose.ankush.storage.room.WeatherDatabase.
WeatherDatabase_Impl does not exist
```

The project compiled and the tests passed, but the app crashed on launch.

**Root cause:** Room was still on 2.5.2. Before 2.7, Room's KSP processor generates **Java** `_Impl` classes. The new `com.android.kotlin.multiplatform.library` plugin compiles `androidMain` as Kotlin only, with no javac step. The generated Java was never compiled into the artifact that `:app` consumes. Old `_Impl` classes still sat under `build/.../javac/debug/`, which made the problem harder to spot.

Upgrading to Room 2.7+ (we use 2.8.4) fixed it, because Room then generates Kotlin. The shared database now looks like this:

```kotlin
@Database(entities = [WeatherEntity::class, AirQualityEntity::class], version = 6, exportSchema = false)
@ConstructedBy(WeatherDatabaseConstructor::class)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao
}

@Suppress("KotlinNoActualForExpect")
expect object WeatherDatabaseConstructor : RoomDatabaseConstructor<WeatherDatabase>
```

The iOS builder uses the bundled SQLite driver and the **same migration objects** as Android:

```kotlin
Room.databaseBuilder<WeatherDatabase>(name = documentDirectory() + "/$WEATHER_DATABASE_NAME")
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
    .fallbackToDestructiveMigration(false)
    .build()
```

Notes from running this in production:

- **Migrations are shared code.** `MIGRATION_5_6` added `latitude`, `longitude` and `placeName` so a cached forecast records which place it belongs to. It's written once and runs on both platforms.
- **Test migrations where they run.** We don't export schemas, so `MigrationTestHelper` isn't available. The migration test is in `storage/src/iosTest` and runs on the iOS simulator, which exercises the real bundled driver.
- **Turn off destructive fallback explicitly.** Losing the cache is bad for an offline-first weather app, and we'd rather a broken migration fail loudly.

> **How to verify:** `WeatherDatabase_Impl.class` should appear in `storage/build/intermediates/runtime_library_classes_jar/androidMain/.../classes.jar`. If it only shows up under a `debug` variant jar, you have this bug.

---

## 6. Networking: explicit auth instead of plugin magic

Ktor replaced Retrofit, with OkHttp on Android and Darwin on iOS. The interesting decision was how to attach auth.

We first used a Ktor auth plugin and replaced it with a plain extension function:

```kotlin
suspend fun HttpClient.authorizedRequest(
    tokenManager: TokenManager,
    block: suspend HttpClient.(HttpRequestBuilder.() -> Unit) -> HttpResponse,
): HttpResponse {
    val firstToken = tokenManager.getStoredToken()
    val response = block(authHeader(firstToken, tokenManager.authorizationScheme()))
    if (response.status != HttpStatusCode.Unauthorized) return response

    // 401 only: a 403 never refreshes or signs out. At most one refresh and one replay per call.
    return when (val result = tokenManager.handleUnauthorized(rejectedToken = firstToken)) {
        is TokenResult.Valid -> {
            // Read the scheme again: the refresh may have moved the session to a DPoP-bound token.
            val retried = block(authHeader(result.token, tokenManager.authorizationScheme()))
            if (retried.status == HttpStatusCode.Unauthorized) endSession(tokenManager)
            retried
        }
        // …
    }
}
```

Why we prefer this to a plugin:

- **The retry policy is in one readable function.** It refreshes at most once and replays at most once, so it can't loop. It reacts only to 401, so a 403 from a wrong password on `DELETE /account` doesn't log the user out.
- **The auth scheme can change partway through a call.** A refresh can upgrade a session from `Bearer` to `DPoP`. Reading the scheme again before the replay is obvious in a function and easy to miss inside plugin config.
- **`rejectedToken` handles concurrent 401s.** `handleUnauthorized` runs under a `Mutex`. When several requests get a 401 together, the first one to take the lock refreshes. The others see that the stored token no longer matches the one they sent, and reuse the new token without refreshing again.

Only small pieces stay platform-specific: the HTTP engine, network connectivity checks, and `Sha256` (an `expect fun` backed by `MessageDigest` on Android and CommonCrypto on iOS).

---

## 7. Presentation: MVI with a pure reducer in `commonMain`

Every feature uses the same shape:

```
Intent (user)  ──►  ViewModel  ──►  Action  ──►  Reducer (pure)  ──►  State
                       │
                       └──►  Effect (one-shot: navigation, toasts) via Channel
```

```kotlin
internal object HomeReducer {
    fun reduce(state: HomeState, action: HomeAction): HomeState = when (action) {
        is HomeAction.WeatherChanged -> reduceWeather(state, action.weather, action.message)
        is HomeAction.Error -> state.copy(isLoading = false, notice = Notice.Error(action.message))
        // …
    }
}

// In the ViewModel, the only place state changes:
private fun dispatch(action: HomeAction) {
    _state.update { HomeReducer.reduce(it, action) }
}
```

With no Android types in it, the reducer runs as a plain `commonTest` on the JVM and on the iOS simulator:

```kotlin
@Test
fun error_clearsLoadingAndReplacesOfflineNotice() {
    val initial = HomeState(isLoading = true, notice = HomeState.Notice.Offline("old offline"))
    val result = HomeReducer.reduce(initial, HomeAction.Error(message = "Boom"))
    assertFalse(result.isLoading)
    assertEquals("Boom", result.error)
}
```

### Model the domain as a stream

The best simplification came from deleting code. Home had three use cases (`GetWeatherReport`, `RefreshWeatherReport`, `GetAirQuality`), and the ViewModel coordinated them: show the cache, resolve the location, fetch, handle a location switch, handle failures. The logic was spread across the ViewModel's `init`, its intent handling, and a few flags.

They were replaced by one use case that returns **one stream**:

```kotlin
internal class ObserveHomeWeather(
    private val repository: WeatherRepository,
    private val coordinates: GetActiveCoordinates,
) {
    operator fun invoke(refreshes: Flow<Boolean>): Flow<HomeWeather> =
        channelFlow {
            val session = Session(channel)
            session.publish { it }
            merge(refreshes, coordinates.changes().map { true })
                .onStart { emit(false) }
                .collectLatest { userInitiated -> session.attempt(userInitiated) }
        }.buffer(Channel.UNLIMITED).distinctUntilChanged()
}
```

What this does:

- **Cache first.** Each attempt shows the Room snapshot before it resolves GPS or calls the network.
- **`collectLatest` cancels the previous attempt.** If the user pins a new place while a fetch is running, the old fetch is cancelled, including its network call. No manual `Job` tracking.
- **The ViewModel becomes a mapper.** It collects `HomeWeather` and dispatches `WeatherChanged`, which the reducer turns into `HomeState`. There's one `isLoading`, derived from `status is Pending || isFetching`.

> **Takeaway:** if your ViewModel coordinates several use cases with flags, the domain probably wants to be a `Flow<DomainState>`. Put the timing logic (cancellation, ordering, cache-then-network) in the use case where it can be tested, and keep the ViewModel as a mapping step.

---

## 8. Deciding where shared code stops

This is the most debated decision in the project, and we changed our minds on it.

**First attempt:** Compose Multiplatform UI on both platforms. iOS hosted the same Compose screens through `ComposeUIViewController`, and `:navigation` had an `expect fun AppNavHost`: Nav3's `NavDisplay` on Android, and a basic stand-in on iOS because `navigation3-ui` has no iOS artifact.

**Where we ended up:** shared state, logic and strings, with native UI on each platform.

- Android draws with Jetpack Compose (most of `:feature:home`'s `androidMain`).
- iOS draws with SwiftUI. iOS no longer hosts any Compose screen.

We wanted iOS to feel native (sheets, glass materials, gestures), and Compose on iOS kept looking slightly off. Moving to SwiftUI didn't require a rewrite, because none of the logic was in the UI layer.

### The iOS bridge: one controller per screen

Each feature has a small class in its `iosMain` that is the SwiftUI screen's only entry point into Kotlin:

```kotlin
class HomeScreenController : KoinComponent {
    private val scope = MainScope()
    private val store = ViewModelStore()
    private val provider = ViewModelProvider.create(store, viewModelFactory {
        initializer { get<HomeViewModel>() }
        initializer { get<NearbyViewModel>() }
        initializer { get<AccountAvatarViewModel>() }
        initializer { get<SavedPlacesViewModel>() }
        initializer { get<AiSummaryViewModel>() }
    })
    // Folds the five ViewModels into one HomeScreenUi StateFlow; exposes tap handlers; close() clears the store.
}
```

[SKIE](https://skie.touchlab.co/) exposes Kotlin `StateFlow`s to Swift as `AsyncSequence`s, so the Swift side is short:

```swift
for await frame in self.controller.state { self.ui = frame }
```

What we learned:

- **The controller owns a real `ViewModelStore`.** Android's lifecycle `ViewModel` is multiplatform now, so iOS uses the same ViewModels as Android, in the same scope, cleared by `close()`.
- **No formatting in Swift.** Dates, units, localized strings and display decisions are built in Kotlin into a `HomeScreenUi` model. Swift renders it. If you add formatting in Swift, you've added a second implementation to maintain.
- **App-wide state gets its own owner.** `AppSession` (in `:app:iosApp`) holds the single `AuthViewModel` and `PaymentViewModel` that sign-in and Settings share. Settings sees them through a `SettingsSession` interface, so `:feature:settings` doesn't depend on the iOS app module.

### The reverse bridge: Swift implementing Kotlin interfaces

Some capabilities exist only in Swift, such as Apple's Foundation Models for on-device AI, the Razorpay iOS SDK, and Firebase Messaging tokens. In these cases Kotlin defines a **callback-based** interface, Swift implements it, and Kotlin wraps it in a Flow:

```kotlin
/** What Swift implements over Apple's Foundation Models. Plain callbacks, because a Kotlin
 *  Flow is awkward to build from Swift; BridgedOnDeviceAiClient turns them into the shared contract. */
interface NativeAiBridge {
    fun availability(): Int
    fun generate(systemInstruction: String, prompt: String,
                 onSnapshot: (String) -> Unit, onDone: (String?) -> Unit): NativeAiCall
}

class BridgedOnDeviceAiClient(private val bridge: NativeAiBridge) : OnDeviceAiClient { /* callbackFlow { … } */ }
```

Android implements the same `OnDeviceAiClient` with ML Kit / Gemini Nano. The `AiSummaryViewModel` in `commonMain` can't tell which model is underneath.

> **Rule of thumb:** Kotlin-to-Swift goes through Flows (SKIE handles them). Swift-to-Kotlin goes through plain callbacks, with Kotlin converting them to Flows. Don't ask Swift to build Kotlin coroutine types.

---

## 9. Navigation and routes

Routes are `@Serializable` `NavKey`s in a tiny `:routing` module:

```kotlin
@Serializable data object HomeRoute : NavKey
@Serializable data object SettingsRoute : NavKey
@Serializable data class LanguageRoute(val languages: List<String>) : NavKey
```

`:navigation` builds the Nav3 `entryProvider` and keeps one back stack per top-level tab. Features don't import each other's screens; they navigate by key.

Two details from the Nav3 migration that are easy to get wrong:

- `entry` is a member of the `entryProvider {}` scope. The IDE likes to auto-import `java.util.Map.entry`, which compiles and then fails in confusing ways.
- `NavDisplay` needs a `LocalNavigationEventDispatcherOwner`. `activity-compose` didn't provide one in the version we used, so `MainActivity` provides it in `setContent`.

---

## 10. Each feature owns its strings

Every feature module keeps its UI strings in Compose Multiplatform resources (`src/commonMain/composeResources/values-*/strings.xml`) in eight locales. The app module no longer passes `XScreenStrings` holder objects down.

- Composables call `stringResource(Res.string.x)`.
- ViewModels call the suspend `getString(Res.string.x)` for snackbar and error text.
- iOS uses the same resources. The iOS controllers resolve strings in Kotlin and hand finished text to Swift.

Pitfalls we hit:

- **Hebrew needs `values-iw` and `values-he`.** Android uses the legacy `iw` code and iOS reports `he`.
- **Format arguments must be positional.** Write `%1$s`, not `%s`.
- **`\'` is not unescaped.** Only `\n`, `\t`, `\uXXXX` and `\\` are, so a literal backslash shows on screen. Use a typographic apostrophe (’).
- **Live language switching on iOS works** because the non-composable `getString` reads `NSLocale.preferredLanguages`. Write `AppleLanguages`, then have screens call `reloadStrings()`.

---

## 11. The build system: where most of the time went

The architecture took weeks. Getting the build to cooperate took longer. Here's the list I wish I'd had at the start.

**AGP 9 and the KMP Android library plugin**
- Move from `kotlin("multiplatform") + com.android.library` to `com.android.kotlin.multiplatform.library`. The Android target is configured inside `kotlin { android { namespace = …; compileSdk = … } }`, and the top-level `android {}` block goes away.
- `androidUnitTest` no longer exists. Use `androidHostTest`, enabled with `withHostTest {}`.
- Remove the `kotlin-android` plugin from app modules, since built-in Kotlin registers the `kotlin` extension itself. Applying both gives `Cannot add extension with name 'kotlin'`.
- Opt-out flags like `android.builtInKotlin=false` work, but they're debt. We removed them as soon as possible.

**Version catalog**
- Moving `buildSrc` `Versions.kt` / `KmmDeps.kt` into `libs.versions.toml` uncovered real drift: `kotlinx-datetime` was on 0.6.2 in `app` and 0.6.1 in four KMP modules, and there was a stale, unused Compose Multiplatform pin. Expect a catalog migration to turn up bugs like these.

**Targets**
- Compose Multiplatform 1.11 stopped publishing `iosX64` (Intel simulator) artifacts. We dropped `iosX64()` everywhere.

**iOS dependencies**
- We moved from the Kotlin CocoaPods plugin to `swiftPMDependencies {}` (Kotlin 2.4.20). The cinterop namespace follows the **Gradle project path**, for example `swiftPMImport.bose.ankush.feature.home.FIRApp`. Read the generated `.def` file instead of guessing.
- If SwiftPM resolution sits at 0% CPU on "Downloading binary artifact…", it's usually waiting on a macOS keychain prompt.

**SKIE**
- Pin SKIE to a release that supports your Kotlin version (0.10.15 was the first to support 2.4.20).
- Swift 6.2 left `#isolation` macro expansions in SKIE's generated-sources folder, which broke later links with "expressions are not allowed at the top level". Our build now deletes that folder before every `KotlinNativeLink`. Also, a "BUILD SUCCEEDED" whose log shows `linkDebugFramework UP-TO-DATE` didn't actually compile SKIE's Swift.

---

## 12. What I'd change, and what's still open

- **Feature-to-feature dependencies.** `home → finder` and `settings → payment` should go through small contract modules, the same way as `:token-storage-api`. Right now a change in `finder` rebuilds `home` on both platforms.
- **Compile-time DI safety.** We traded it for multiplatform support. Koin's verify tests recover part of it, and we should add them to every feature.
- **`:navigation` still carries an iOS stand-in** for `NavDisplay`, even though iOS has moved to SwiftUI. Navigation is now effectively Android-only and the module's `iosMain` should shrink to match.
- **Shared UI isn't the goal.** About 10k lines of `androidMain` Compose UI isn't a sign the migration is unfinished. Shared state and native UI are the intended end state.

---

## Summary

| Decision | What we chose | Why |
|---|---|---|
| Migration order | Data layer, then DI, then features, then shell | Each step ships alone; nothing is half-done |
| Module contracts | Interface-only modules (`:token-storage-api`) | Fewer graph edges, less to build per target |
| DI | Koin, three modules per feature (`domain` / `viewModel` / `expect platform`) | Hilt is JVM-only and can't aggregate from KMP libraries |
| Persistence | Room KMP 2.7+, shared migrations, bundled SQLite on iOS | Room < 2.7 generates Java, which the KMP plugin never compiles |
| Networking | Ktor with an explicit `authorizedRequest`, DPoP via hardware keys | Readable retry policy; scheme can change mid-call |
| Presentation | MVI, pure reducer in `commonMain`, domain as a `Flow` | Testable on every platform; timing logic leaves the ViewModel |
| UI | Native per platform (Compose / SwiftUI) over shared UI models | Native feel; formatting lives in one place |
| iOS bridge | One `XScreenController` per screen; SKIE for Flows; callbacks for Swift-to-Kotlin | Small, explicit surface between the languages |
| Resources | Compose Multiplatform resources owned by each feature | Strings live with the feature, on both platforms |

---

### Check your understanding

If you're using this post to learn, try answering these without scrolling back up:

1. Why did `WeatherDatabase_Impl` exist on disk but not at runtime?
2. What problem does `:token-storage-api` solve that a plain interface inside `:storage` doesn't?
3. Why does `authorizedRequest` read `authorizationScheme()` a second time before replaying?
4. What does `collectLatest` in `ObserveHomeWeather` give you that you'd otherwise build by hand?
5. Why does Swift implement `NativeAiBridge` with callbacks instead of returning a Flow?
6. What compile-time guarantee did leaving Hilt cost, and how would you get some of it back?

*Every snippet above is taken from the Weatherify codebase, sometimes trimmed for length.*
