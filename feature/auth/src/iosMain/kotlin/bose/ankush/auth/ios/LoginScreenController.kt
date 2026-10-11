package bose.ankush.auth.ios

import bose.ankush.analytics.AnalyticsEvent
import bose.ankush.analytics.AnalyticsTracker
import bose.ankush.auth.generated.resources.Res
import bose.ankush.auth.generated.resources.login_create_account_title
import bose.ankush.auth.generated.resources.login_email_label
import bose.ankush.auth.generated.resources.login_error_email_empty
import bose.ankush.auth.generated.resources.login_error_email_invalid
import bose.ankush.auth.generated.resources.login_error_password_empty
import bose.ankush.auth.generated.resources.login_error_password_short
import bose.ankush.auth.generated.resources.login_join_community_subtitle
import bose.ankush.auth.generated.resources.login_password_hide_btn
import bose.ankush.auth.generated.resources.login_password_label
import bose.ankush.auth.generated.resources.login_password_show_btn
import bose.ankush.auth.generated.resources.login_photo_credit
import bose.ankush.auth.generated.resources.login_privacy_link_txt
import bose.ankush.auth.generated.resources.login_signin_btn
import bose.ankush.auth.generated.resources.login_signin_subtitle
import bose.ankush.auth.generated.resources.login_terms_agreement_txt
import bose.ankush.auth.generated.resources.login_terms_link_txt
import bose.ankush.auth.generated.resources.login_toggle_to_login_txt
import bose.ankush.auth.generated.resources.login_toggle_to_register_txt
import bose.ankush.auth.generated.resources.login_welcome_back_title
import bose.ankush.auth.generated.resources.login_welcome_handoff
import bose.ankush.auth.generated.resources.login_welcome_headline
import bose.ankush.auth.generated.resources.login_welcome_tagline
import bose.ankush.auth.presentation.AuthIntent
import bose.ankush.auth.presentation.AuthState
import bose.ankush.auth.presentation.AuthViewModel
import bose.ankush.auth.presentation.LoginInputError
import bose.ankush.auth.presentation.validateLogin
import bose.ankush.network.api.UnsplashApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/** Copy for the SwiftUI sign-in screen, from this module's translations. */
data class LoginStrings(
    val headline: String,
    /** Shown on the forest for a moment after signing in, before Home. */
    val welcome: String,
    val tagline: String,
    val toggleToRegister: String,
    val toggleToLogin: String,
    val signInTitle: String,
    val signInSubtitle: String,
    val registerTitle: String,
    val registerSubtitle: String,
    val signInMode: String,
    val registerMode: String,
    val email: String,
    val password: String,
    val showPassword: String,
    val hidePassword: String,
    val submit: String,
    val termsLink: String,
    val privacyLink: String,
    /** The agreement line with both link texts already in place. */
    val agreement: String,
    val termsUrl: String,
    val privacyUrl: String,
)

data class LoginScreenUi(
    val strings: LoginStrings,
    val isLoading: Boolean,
    /** Today's forest, or null offline (the screen shows its gradient). */
    val photo: LoginPhotoUi?,
)

data class LoginPhotoUi(
    val imageUrl: String,
    val credit: String,
    val profileUrl: String,
    val unsplashUrl: String,
)

/** A failed sign-in, an expired session, or a deleted account, for Swift to show as a banner. */
data class LoginNotice(
    val message: String,
    val isWarning: Boolean,
)

/**
 * The SwiftUI sign-in screen's entry into shared code: the same [AuthViewModel] the rest of the
 * app observes (so a successful sign-in switches the root to Home), the shared form rules, and
 * the auth module's copy. Swift calls [close] when the screen goes away.
 */
class LoginScreenController(
    private val auth: AuthViewModel,
) : KoinComponent {
    private val scope = MainScope()
    private val strings = MutableStateFlow<LoginStrings?>(null)
    private val notices = Channel<LoginNotice>(Channel.BUFFERED)
    private val forest = DailyForestPhoto(get<UnsplashApi>())
    private val photo = MutableStateFlow<ForestPhoto?>(null)
    private val photoUi = MutableStateFlow<LoginPhotoUi?>(null)

    val state: StateFlow<LoginScreenUi?> =
        combine(strings.filterNotNull(), auth.authState, photoUi) { copy, authState, backdrop ->
            LoginScreenUi(
                strings = copy,
                isLoading = authState is AuthState.Loading,
                photo = backdrop
            )
        }.stateIn(scope, SharingStarted.Eagerly, null)

    /** Server errors, session expiry, and account deletion. */
    val events: Flow<LoginNotice> = notices.receiveAsFlow()

    init {
        get<AnalyticsTracker>().track(AnalyticsEvent.ScreenView("login", "LoginScreen"))
        scope.launch { strings.value = loadStrings() }
        scope.launch {
            val today = forest.today() ?: return@launch
            photo.value = today
            photoUi.value =
                LoginPhotoUi(
                    imageUrl = today.imageUrl,
                    credit = getString(Res.string.login_photo_credit, today.photographer),
                    profileUrl = today.profileUrl,
                    unsplashUrl = UNSPLASH_URL,
                )
        }
        scope.launch {
            auth.authState.collect { authState ->
                when (authState) {
                    is AuthState.Error -> notify(LoginNotice(authState.message, isWarning = false))
                    is AuthState.SessionExpired -> notify(
                        LoginNotice(
                            authState.message,
                            isWarning = true
                        )
                    )

                    // Not an error: the Swift banner treats every non-warning notice as one.
                    is AuthState.AccountDeleted -> notify(
                        LoginNotice(
                            authState.message,
                            isWarning = true
                        )
                    )

                    else -> Unit
                }
            }
        }
    }

    /**
     * The localized reason the form can't be sent, or null when it is fine. Same rules as the
     * Android screen ([validateLogin]).
     */
    suspend fun validate(
        email: String,
        password: String,
    ): String? =
        when (validateLogin(email, password)) {
            LoginInputError.EMAIL_EMPTY -> getString(Res.string.login_error_email_empty)
            LoginInputError.EMAIL_INVALID -> getString(Res.string.login_error_email_invalid)
            LoginInputError.PASSWORD_EMPTY -> getString(Res.string.login_error_password_empty)
            LoginInputError.PASSWORD_SHORT -> getString(Res.string.login_error_password_short)
            null -> null
        }

    /** Signs in, or creates the account first when [register] is set. Ignored while a request runs. */
    fun submit(
        email: String,
        password: String,
        register: Boolean,
    ) {
        if (auth.authState.value is AuthState.Loading || validateLogin(
                email,
                password
            ) != null
        ) return
        val trimmed = email.trim()
        auth.processIntent(
            if (register) AuthIntent.Register(trimmed, password) else AuthIntent.Login(
                trimmed,
                password
            ),
        )
    }

    /** Call once the photo is actually on screen. */
    fun photoShown() {
        val shown = photo.value ?: return
        scope.launch { forest.trackShown(shown) }
    }

    fun close() = scope.cancel()

    private suspend fun notify(notice: LoginNotice) {
        notices.send(notice)
        // Back to idle, so the same error can show again on the next attempt.
        auth.processIntent(AuthIntent.Reset)
    }

    private suspend fun loadStrings(): LoginStrings {
        val terms = getString(Res.string.login_terms_link_txt)
        val privacy = getString(Res.string.login_privacy_link_txt)
        return LoginStrings(
            headline = getString(Res.string.login_welcome_headline),
            welcome = getString(Res.string.login_welcome_handoff),
            tagline = getString(Res.string.login_welcome_tagline),
            toggleToRegister = getString(Res.string.login_toggle_to_register_txt),
            toggleToLogin = getString(Res.string.login_toggle_to_login_txt),
            signInTitle = getString(Res.string.login_welcome_back_title),
            signInSubtitle = getString(Res.string.login_signin_subtitle),
            registerTitle = getString(Res.string.login_create_account_title),
            registerSubtitle = getString(Res.string.login_join_community_subtitle),
            signInMode = getString(Res.string.login_signin_btn),
            registerMode = getString(Res.string.login_create_account_title),
            email = getString(Res.string.login_email_label),
            password = getString(Res.string.login_password_label),
            showPassword = getString(Res.string.login_password_show_btn),
            hidePassword = getString(Res.string.login_password_hide_btn),
            submit = getString(Res.string.login_signin_btn),
            termsLink = terms,
            privacyLink = privacy,
            agreement = getString(Res.string.login_terms_agreement_txt, terms, privacy),
            // Same pages the Android sign-in screen opens.
            termsUrl = "https://data.androidplay.in/wfy/terms-and-conditions",
            privacyUrl = "https://data.androidplay.in/wfy/privacy-policy",
        )
    }
}

private const val UNSPLASH_URL = "https://unsplash.com/?utm_source=weatherify&utm_medium=referral"
