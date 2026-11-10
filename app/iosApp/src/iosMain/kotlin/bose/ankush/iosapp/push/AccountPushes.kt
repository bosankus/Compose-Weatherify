package bose.ankush.iosapp.push

import bose.ankush.network.auth.repository.AuthRepository
import bose.ankush.network.repository.ACCOUNT_PHOTO_CHANGED_PUSH
import bose.ankush.network.repository.AccountRepository
import bose.ankush.network.repository.PushTokenProvider
import bose.ankush.network.repository.PushTokenRegistration
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.coroutines.resume

/**
 * What Swift implements over Firebase Messaging, whose iOS SDK is only reachable from Swift here.
 * Plain callbacks, like NativeAiBridge.
 */
interface NativePushTokens {
    /** This device's FCM token, or null while APNs hasn't handed the app a device token yet. */
    fun fetchToken(completion: (String?) -> Unit)

    /** Drops this device's token, so a new one (not tied to a deleted account) is issued. */
    fun deleteToken()
}

/** Binds [PushTokenRegistration] to the Swift token source, as appPushKoinModule does on Android. */
internal fun iosPushModule(tokens: NativePushTokens): Module =
    module {
        single { tokens }
        single<PushTokenProvider> {
            PushTokenProvider {
                suspendCancellableCoroutine { cont ->
                    tokens.fetchToken { token -> if (cont.isActive) cont.resume(token) }
                }
            }
        }
        single { PushTokenRegistration(get(), get()) }
    }

/**
 * Swift's app delegate hands every FCM event here; the iOS counterpart of Android's
 * WeatherifyApplication push registration and WeatherifyMessagingService.
 */
class AccountPushes : KoinComponent {
    private val scope = MainScope()
    private val authRepository: AuthRepository by inject()
    private val accountRepository: AccountRepository by inject()
    private val registration: PushTokenRegistration by inject()

    /** On every launch while signed in and after each sign-in; the server ignores repeats. */
    fun start() {
        scope.launch {
            authRepository
                .isLoggedIn()
                .distinctUntilChanged()
                .filter { it }
                .collect { registration.register() }
        }
    }

    /**
     * FCM issued or rotated the token. At launch this usually comes after [start]'s first
     * attempt, which found no token because APNs hadn't answered yet.
     */
    fun tokenRefreshed(token: String) {
        scope.launch {
            if (authRepository.isLoggedIn().first()) registration.register(token)
        }
    }

    /**
     * A silent push of [type]. Returns whether it fetched new data, for the background fetch result
     * iOS asks for. iOS gives the app about 30 seconds, so the fetch is capped well under that.
     */
    suspend fun handleSilentPush(type: String?): Boolean =
        when (type) {
            // The photo changed on another device. Fetching the account publishes the new photo
            // URL to every screen, and its new path drops the old photo from the cache.
            ACCOUNT_PHOTO_CHANGED_PUSH ->
                withTimeoutOrNull(ACCOUNT_REFRESH_TIMEOUT_MS) { accountRepository.getAccount() }
                    ?.isSuccess == true

            else -> false
        }

    private companion object {
        const val ACCOUNT_REFRESH_TIMEOUT_MS = 20_000L
    }
}
