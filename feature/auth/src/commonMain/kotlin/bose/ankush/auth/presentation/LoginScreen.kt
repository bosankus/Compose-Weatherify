package bose.ankush.auth.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bose.ankush.auth.backdrop.LoginBackdropViewModel
import bose.ankush.auth.backdrop.LoginPhotoUi
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
import bose.ankush.auth.generated.resources.login_privacy_link_txt
import bose.ankush.auth.generated.resources.login_signin_btn
import bose.ankush.auth.generated.resources.login_signin_subtitle
import bose.ankush.auth.generated.resources.login_terms_agreement_txt
import bose.ankush.auth.generated.resources.login_terms_link_txt
import bose.ankush.auth.generated.resources.login_toggle_to_login_txt
import bose.ankush.auth.generated.resources.login_toggle_to_register_txt
import bose.ankush.auth.generated.resources.login_welcome_back_title
import bose.ankush.auth.generated.resources.login_welcome_headline
import bose.ankush.auth.generated.resources.login_welcome_tagline
import bose.ankush.commonui.theme.LightSystemBarIcons
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.PI
import kotlin.math.sin

private const val TERMS_URL = "https://data.androidplay.in/wfy/terms-and-conditions"
private const val PRIVACY_URL = "https://data.androidplay.in/wfy/privacy-policy"

/**
 * Welcome screen, the Compose twin of the iOS one: today's forest full-bleed, a small leaf
 * wordmark, one calm headline and two buttons; the form slides up in a sheet.
 *
 * It opens on the splash: same grey, same leaf in the same spot. The leaf then lifts away
 * while the forest fades in, so the system splash hands over without a cut.
 */
@Composable
fun LoginScreen(
    onLoginClick: (email: String, password: String) -> Unit,
    onRegisterClick: (email: String, password: String) -> Unit,
    onWebUrlClick: (url: String) -> Unit = {},
    isLoading: Boolean = false,
) {
    val backdrop = koinViewModel<LoginBackdropViewModel>()
    val photo by backdrop.photo.collectAsState()
    LoginContent(
        photo = photo,
        isLoading = isLoading,
        onSubmit = { email, password, register ->
            if (register) onRegisterClick(email, password) else onLoginClick(email, password)
        },
        onWebUrlClick = onWebUrlClick,
        onPhotoShown = backdrop::photoShown,
    )
}

private enum class LoginMode { SignIn, Register }

@Composable
private fun LoginContent(
    photo: LoginPhotoUi?,
    isLoading: Boolean,
    onSubmit: (email: String, password: String, register: Boolean) -> Unit,
    onWebUrlClick: (String) -> Unit,
    onPhotoShown: () -> Unit,
) {
    LightSystemBarIcons()
    var mode by rememberSaveable { mutableStateOf<LoginMode?>(null) }

    Box(Modifier.fillMaxSize()) {
        ForestBackdrop(photo = photo, onShown = onPhotoShown)
        SplashLeafHandoff()
        Welcome(
            photo = photo,
            onSignIn = { mode = LoginMode.SignIn },
            onRegister = { mode = LoginMode.Register },
            onWebUrlClick = onWebUrlClick,
        )
        FormSheet(
            mode = mode,
            isLoading = isLoading,
            onModeChange = { mode = it },
            onDismiss = { if (!isLoading) mode = null },
            onSubmit = onSubmit,
        )
    }
}

// region Backdrop

/**
 * Today's forest, slowly drifting closer. Until it loads, or when offline, the splash grey
 * deepening downwards, so the first frame matches the system splash exactly.
 */
@Composable
private fun ForestBackdrop(
    photo: LoginPhotoUi?,
    onShown: () -> Unit,
) {
    var loaded by remember(photo?.imageUrl) { mutableStateOf(false) }
    val photoAlpha by animateFloatAsState(
        targetValue = if (loaded) 1f else 0f,
        animationSpec = tween(PHOTO_FADE_MS, easing = FastOutSlowInEasing),
    )
    val drift by rememberInfiniteTransition().animateFloat(
        initialValue = 1f,
        targetValue = DRIFT_SCALE,
        animationSpec = infiniteRepeatable(tween(DRIFT_MS, easing = LinearEasing), RepeatMode.Reverse),
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SplashGrey, SplashGreyDeep, SplashGreyNight))),
    ) {
        if (photo != null) {
            AsyncImage(
                model = photo.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onSuccess = {
                    loaded = true
                    onShown()
                },
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = photoAlpha
                            scaleX = drift
                            scaleY = drift
                        },
            )
        }
        // Soft at the top for the wordmark, deep at the bottom for the copy and buttons.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.35f),
                    0.22f to Color.Transparent,
                    0.42f to Color.Transparent,
                    0.68f to Color.Black.copy(alpha = 0.55f),
                    1f to Color.Black.copy(alpha = 0.85f),
                ),
            ),
        )
    }
}

/**
 * The splash leaf, drawn at the splash icon's size and centre so the first frame lines up
 * with the system splash, then lifted and faded away.
 */
@Composable
private fun SplashLeafHandoff() {
    var gone by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { gone = true }
    val progress by animateFloatAsState(
        targetValue = if (gone) 1f else 0f,
        animationSpec = tween(LEAF_HANDOFF_MS, delayMillis = LEAF_HOLD_MS, easing = FastOutSlowInEasing),
    )
    if (progress >= 1f) return
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            painter = rememberVectorPainter(SplashLeaf),
            contentDescription = null,
            modifier =
                Modifier
                    .size(SPLASH_ICON_SIZE)
                    .graphicsLayer {
                        alpha = 1f - progress
                        translationY = -progress * 48.dp.toPx()
                        scaleX = 1f - progress * 0.15f
                        scaleY = 1f - progress * 0.15f
                    },
        )
    }
}

// endregion

// region Welcome

@Composable
private fun Welcome(
    photo: LoginPhotoUi?,
    onSignIn: () -> Unit,
    onRegister: () -> Unit,
    onWebUrlClick: (String) -> Unit,
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val copyIn by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(APPEAR_MS, delayMillis = APPEAR_DELAY_MS, easing = FastOutSlowInEasing),
    )
    val buttonsIn by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(APPEAR_MS, delayMillis = APPEAR_DELAY_MS + 120, easing = FastOutSlowInEasing),
    )

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 12.dp),
    ) {
        // The leaf wordmark at the start, the photo credit at the end.
        Row(verticalAlignment = Alignment.Top) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(copyIn),
            ) {
                Image(
                    painter = rememberVectorPainter(WordmarkLeaf),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "Weatherify",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.weight(1f))
            Crossfade(targetState = photo, label = "credit") { shown ->
                if (shown != null) PhotoCredit(shown, onWebUrlClick)
            }
        }

        Spacer(Modifier.weight(1f))

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier =
                Modifier
                    .padding(bottom = 32.dp)
                    .graphicsLayer {
                        alpha = copyIn
                        translationY = (1f - copyIn) * 16.dp.toPx()
                    },
        ) {
            Text(
                text = stringResource(Res.string.login_welcome_headline),
                color = Color.White,
                fontSize = 38.sp,
                lineHeight = 44.sp,
                fontFamily = FontFamily.Serif,
            )
            Text(
                text = stringResource(Res.string.login_welcome_tagline),
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 16.sp,
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier =
                Modifier.graphicsLayer {
                    alpha = buttonsIn
                    translationY = (1f - buttonsIn) * 24.dp.toPx()
                },
        ) {
            PillButton(
                text = stringResource(Res.string.login_signin_btn),
                onClick = onSignIn,
            )
            PillButton(
                text = stringResource(Res.string.login_create_account_title),
                onClick = onRegister,
                filled = false,
            )
        }

        AgreementText(
            onWebUrlClick = onWebUrlClick,
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
        )
    }
}

/** Unsplash's required credit: small, faint, top end, linking the photographer and Unsplash. */
@Composable
private fun PhotoCredit(
    photo: LoginPhotoUi,
    onWebUrlClick: (String) -> Unit,
) {
    val text =
        remember(photo) {
            linkify(
                photo.credit,
                listOfNotNull(
                    photo.credit.photographerRange()?.let { it to photo.profileUrl },
                    photo.credit.rangeOf("Unsplash")?.let { it to photo.unsplashUrl },
                ),
                onWebUrlClick,
                underline = false,
            )
        }
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.5f),
        fontSize = 11.sp,
        lineHeight = 15.sp,
        textAlign = TextAlign.End,
        maxLines = 2,
        modifier = Modifier.widthIn(max = 170.dp),
    )
}

@Composable
private fun AgreementText(
    onWebUrlClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val terms = stringResource(Res.string.login_terms_link_txt)
    val privacy = stringResource(Res.string.login_privacy_link_txt)
    val full = stringResource(Res.string.login_terms_agreement_txt, terms, privacy)
    val text =
        remember(full) {
            linkify(
                full,
                listOfNotNull(
                    full.rangeOf(terms)?.let { it to TERMS_URL },
                    full.rangeOf(privacy)?.let { it to PRIVACY_URL },
                ),
                onWebUrlClick,
                underline = true,
            )
        }
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.6f),
        fontSize = 12.sp,
        lineHeight = 18.sp,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

// endregion

// region Form sheet

/**
 * The sign-in or sign-up form, sliding up over the forest. Drawn in the same window as the
 * rest of the app, so the app's toast for a server error still shows above it.
 */
@Composable
private fun FormSheet(
    mode: LoginMode?,
    isLoading: Boolean,
    onModeChange: (LoginMode) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (email: String, password: String, register: Boolean) -> Unit,
) {
    LoginBackHandler(enabled = mode != null, onBack = onDismiss)

    AnimatedVisibility(visible = mode != null, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        // Keeps the last mode while the sheet slides out, so its copy doesn't blank mid-exit.
        var shownMode by remember { mutableStateOf(LoginMode.SignIn) }
        if (mode != null) shownMode = mode
        AnimatedVisibility(
            visible = mode != null,
            enter = slideInVertically(tween(SHEET_MS, easing = FastOutSlowInEasing)) { it },
            exit = slideOutVertically(tween(SHEET_MS, easing = FastOutSlowInEasing)) { it },
        ) {
            FormSheetBody(
                mode = shownMode,
                isLoading = isLoading,
                onModeChange = onModeChange,
                onSubmit = onSubmit,
            )
        }
    }
}

@Composable
private fun FormSheetBody(
    mode: LoginMode,
    isLoading: Boolean,
    onModeChange: (LoginMode) -> Unit,
    onSubmit: (email: String, password: String, register: Boolean) -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var shakes by remember { mutableIntStateOf(0) }
    val shake = remember { Animatable(0f) }
    val focusManager = LocalFocusManager.current
    val emailFocus = remember { FocusRequester() }
    val registering = mode == LoginMode.Register

    val errors =
        mapOf(
            LoginInputError.EMAIL_EMPTY to stringResource(Res.string.login_error_email_empty),
            LoginInputError.EMAIL_INVALID to stringResource(Res.string.login_error_email_invalid),
            LoginInputError.PASSWORD_EMPTY to stringResource(Res.string.login_error_password_empty),
            LoginInputError.PASSWORD_SHORT to stringResource(Res.string.login_error_password_short),
        )

    LaunchedEffect(shakes) {
        if (shakes > 0) {
            shake.snapTo(0f)
            shake.animateTo(1f, tween(SHAKE_MS, easing = LinearEasing))
        }
    }
    LaunchedEffect(Unit) { emailFocus.requestFocus() }

    val submit: () -> Unit = {
        val problem = validateLogin(email, password)
        if (problem != null) {
            error = errors[problem]
            shakes++
        } else if (!isLoading) {
            error = null
            focusManager.clearFocus()
            onSubmit(email.trim(), password, registering)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(22.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(SheetShape)
                .background(SheetColor)
                // Swallow taps so they don't reach the scrim and close the sheet.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ).navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 12.dp, bottom = 24.dp),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 36.dp, height = 5.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.25f)),
        )

        Crossfade(targetState = registering, label = "title") { reg ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text =
                        stringResource(
                            if (reg) Res.string.login_create_account_title else Res.string.login_welcome_back_title,
                        ),
                    color = Color.White,
                    fontSize = 30.sp,
                    fontFamily = FontFamily.Serif,
                )
                Text(
                    text =
                        stringResource(
                            if (reg) Res.string.login_join_community_subtitle else Res.string.login_signin_subtitle,
                        ),
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 15.sp,
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier =
                Modifier.graphicsLayer {
                    translationX = 8.dp.toPx() * sin(shake.value * PI.toFloat() * 6)
                },
        ) {
            QuietField(
                value = email,
                onValueChange = {
                    email = it
                    error = null
                },
                icon = { Icon(Icons.Outlined.Email, null, tint = FieldIcon, modifier = Modifier.size(18.dp)) },
                placeholder = stringResource(Res.string.login_email_label),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.focusRequester(emailFocus),
            )
            QuietField(
                value = password,
                onValueChange = {
                    password = it
                    error = null
                },
                icon = { Icon(Icons.Outlined.Lock, null, tint = FieldIcon, modifier = Modifier.size(18.dp)) },
                placeholder = stringResource(Res.string.login_password_label),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { submit() }),
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailing = {
                    Text(
                        text =
                            stringResource(
                                if (showPassword) Res.string.login_password_hide_btn else Res.string.login_password_show_btn,
                            ),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier =
                            Modifier
                                .clip(CircleShape)
                                .clickable { showPassword = !showPassword }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                    )
                },
            )
        }

        AnimatedVisibility(visible = error != null, enter = fadeIn(), exit = fadeOut()) {
            Text(text = error.orEmpty(), color = ErrorInk, fontSize = 13.sp)
        }

        PillButton(
            text =
                stringResource(
                    if (registering) Res.string.login_create_account_title else Res.string.login_signin_btn,
                ),
            onClick = submit,
            loading = isLoading,
        )

        Text(
            text =
                stringResource(
                    if (registering) Res.string.login_toggle_to_login_txt else Res.string.login_toggle_to_register_txt,
                ),
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .clickable(enabled = !isLoading) {
                        error = null
                        onModeChange(if (registering) LoginMode.SignIn else LoginMode.Register)
                    }.padding(vertical = 8.dp),
        )
    }
}

/** Low-contrast field: a faint fill, a glyph, no underline. */
@Composable
private fun QuietField(
    value: String,
    onValueChange: (String) -> Unit,
    icon: @Composable () -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions,
    modifier: Modifier = Modifier,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: @Composable (() -> Unit)? = null,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
        cursorBrush = SolidColor(Color.White),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier =
                    Modifier
                        .heightIn(min = 56.dp)
                        .clip(FieldShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 18.dp),
            ) {
                icon()
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(placeholder, color = Color.White.copy(alpha = 0.45f), fontSize = 16.sp)
                    }
                    inner()
                }
                trailing?.invoke()
            }
        },
    )
}

// endregion

// region Small pieces

/** White pill for the main action; a glassy outline for the secondary one. Shrinks a touch on press. */
@Composable
private fun PillButton(
    text: String,
    onClick: () -> Unit,
    filled: Boolean = true,
    loading: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.98f else 1f, tween(150))
    val shape: Shape = CircleShape
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .scale(press)
                .clip(shape)
                .then(
                    if (filled) {
                        Modifier.background(Color.White)
                    } else {
                        Modifier
                            .background(Color.White.copy(alpha = 0.14f))
                            .border(1.dp, Color.White.copy(alpha = 0.28f), shape)
                    },
                ).clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = !loading,
                    onClick = onClick,
                ),
    ) {
        Text(
            text = text,
            color = if (filled) ButtonInk else Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.alpha(if (loading) 0f else 1f),
        )
        if (loading) {
            CircularProgressIndicator(
                color = ButtonInk,
                strokeWidth = 2.dp,
                strokeCap = StrokeCap.Round,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** Turns each (range, url) into a link that opens in the app's web view. */
private fun linkify(
    text: String,
    links: List<Pair<IntRange, String>>,
    onClick: (String) -> Unit,
    underline: Boolean,
): AnnotatedString =
    buildAnnotatedString {
        val style =
            TextLinkStyles(
                SpanStyle(
                    color = Color.White.copy(alpha = 0.88f),
                    textDecoration = if (underline) TextDecoration.Underline else null,
                ),
            )
        var cursor = 0
        links.sortedBy { it.first.first }.forEach { (range, url) ->
            if (range.first < cursor) return@forEach
            append(text.substring(cursor, range.first))
            withLink(LinkAnnotation.Clickable(url, style) { onClick(url) }) {
                append(text.substring(range.first, range.last + 1))
            }
            cursor = range.last + 1
        }
        append(text.substring(cursor))
    }

private fun String.rangeOf(part: String): IntRange? = indexOf(part).takeIf { it >= 0 }?.let { it until it + part.length }

/** The name between "by " and " on" in the English credit; other languages link Unsplash only. */
private fun String.photographerRange(): IntRange? {
    val start = indexOf("by ").takeIf { it >= 0 }?.plus(3) ?: return null
    val end = indexOf(" on", start).takeIf { it > start } ?: return null
    return start until end
}

/** The splash leaf at the splash canvas's own 288 viewport, so it lines up 1:1 with it. */
private val SplashLeaf: ImageVector by lazy {
    ImageVector
        .Builder("SplashLeaf", 288.dp, 288.dp, 288f, 288f)
        .path(fill = SolidColor(Color(0xF2FFFFFF))) {
            moveTo(117.12f, 170.88f)
            curveTo(107.52f, 132.48f, 139.2f, 110.4f, 174.72f, 113.28f)
            curveTo(177.6f, 148.8f, 151.68f, 180.48f, 117.12f, 170.88f)
            close()
        }.path(stroke = SolidColor(SplashGrey), strokeLineWidth = 4f, strokeLineCap = StrokeCap.Round) {
            moveTo(105.6f, 182.4f)
            lineTo(155.52f, 132.48f)
        }.build()
}

/** The same leaf cropped tight for the wordmark; the vein is cut out of the leaf's colour. */
private val WordmarkLeaf: ImageVector by lazy {
    ImageVector
        .Builder("WordmarkLeaf", 18.dp, 18.dp, 76f, 76f)
        .path(fill = SolidColor(Color.White.copy(alpha = 0.92f))) {
            moveTo(15.12f, 62.88f)
            curveTo(5.52f, 24.48f, 37.2f, 2.4f, 72.72f, 5.28f)
            curveTo(75.6f, 40.8f, 49.68f, 72.48f, 15.12f, 62.88f)
            close()
        }.path(stroke = SolidColor(Color.Black.copy(alpha = 0.35f)), strokeLineWidth = 3f, strokeLineCap = StrokeCap.Round) {
            moveTo(3.6f, 74.4f)
            lineTo(53.52f, 24.48f)
        }.build()
}

// The system splash background (@color/splash_background), then two deeper steps of it.
private val SplashGrey = Color(0xFF616761)
private val SplashGreyDeep = Color(0xFF454A45)
private val SplashGreyNight = Color(0xFF232623)

private val SheetColor = Color(0xF21B1E1B)
private val ButtonInk = Color(0xFF10241A)
private val FieldIcon = Color.White.copy(alpha = 0.5f)
private val ErrorInk = Color(0xFFFFB4A9)
private val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
private val FieldShape = RoundedCornerShape(16.dp)

// Android 12+ draws the splash icon in a 240dp box (the 288dp canvas minus its mask inset).
private val SPLASH_ICON_SIZE: Dp = 240.dp
private const val DRIFT_SCALE = 1.08f
private const val DRIFT_MS = 30_000
private const val PHOTO_FADE_MS = 1_200
private const val LEAF_HOLD_MS = 150
private const val LEAF_HANDOFF_MS = 700
private const val APPEAR_MS = 900
private const val APPEAR_DELAY_MS = 350
private const val SHEET_MS = 320
private const val SHAKE_MS = 400

// endregion
