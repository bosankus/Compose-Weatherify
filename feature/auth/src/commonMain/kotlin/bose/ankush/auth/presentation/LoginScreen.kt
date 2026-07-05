package bose.ankush.auth.presentation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
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
import org.jetbrains.compose.resources.stringResource

private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

@Composable
fun LoginScreen(
    onLoginClick: (email: String, password: String) -> Unit,
    onRegisterClick: (email: String, password: String) -> Unit,
    onWebUrlClick: (url: String) -> Unit = {},
    isLoading: Boolean = false,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoginMode by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var isTitleClicked by remember { mutableStateOf(false) }
    var isSubtitleClicked by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    val emailEmptyError = stringResource(Res.string.login_error_email_empty)
    val emailInvalidError = stringResource(Res.string.login_error_email_invalid)
    val passwordEmptyError = stringResource(Res.string.login_error_password_empty)
    val passwordShortError = stringResource(Res.string.login_error_password_short)

    val welcomeBackTitle = stringResource(Res.string.login_welcome_back_title)
    val createAccountTitle = stringResource(Res.string.login_create_account_title)
    val signInSubtitle = stringResource(Res.string.login_signin_subtitle)
    val joinCommunitySubtitle = stringResource(Res.string.login_join_community_subtitle)
    val signInBtnText = stringResource(Res.string.login_signin_btn)
    val hidePasswordText = stringResource(Res.string.login_password_hide_btn)
    val showPasswordText = stringResource(Res.string.login_password_show_btn)
    val toggleToRegisterText = stringResource(Res.string.login_toggle_to_register_txt)
    val toggleToLoginText = stringResource(Res.string.login_toggle_to_login_txt)
    val termsLinkText = stringResource(Res.string.login_terms_link_txt)
    val privacyLinkText = stringResource(Res.string.login_privacy_link_txt)
    val termsAgreementText =
        stringResource(Res.string.login_terms_agreement_txt, termsLinkText, privacyLinkText)

    val isEmailValid = { input: String -> EMAIL_REGEX.matches(input) }
    val isPasswordValid = { input: String -> input.length >= 6 }

    val validateInputs = {
        when {
            email.isBlank() -> {
                errorMessage = emailEmptyError
                false
            }
            !isEmailValid(email) -> {
                errorMessage = emailInvalidError
                false
            }
            password.isBlank() -> {
                errorMessage = passwordEmptyError
                false
            }
            !isPasswordValid(password) -> {
                errorMessage = passwordShortError
                false
            }
            else -> {
                errorMessage = null
                true
            }
        }
    }

    val handleSubmit = {
        if (!isLoading && validateInputs()) {
            if (isLoginMode) onLoginClick(email, password) else onRegisterClick(email, password)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 80.dp),
            ) {
                val titleScale by animateFloatAsState(
                    targetValue = if (isTitleClicked) 1.1f else 1.0f,
                    animationSpec = spring(dampingRatio = 0.4f, stiffness = 300f),
                    label = "titleScale",
                )
                val titleColor =
                    if (isTitleClicked) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    }

                Text(
                    text = if (isLoginMode) welcomeBackTitle else createAccountTitle,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = titleColor,
                    textAlign = TextAlign.Start,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .scale(titleScale)
                            .clickable { isTitleClicked = !isTitleClicked },
                )

                val subtitleScale by animateFloatAsState(
                    targetValue = if (isSubtitleClicked) 1.1f else 1.0f,
                    animationSpec =
                        tween(
                            durationMillis = 300,
                            easing = FastOutSlowInEasing,
                        ),
                    label = "subtitleScale",
                )
                val subtitleColor =
                    if (isSubtitleClicked) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    }

                Text(
                    text = if (isLoginMode) signInSubtitle else joinCommunitySubtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = subtitleColor,
                    textAlign = TextAlign.Start,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .scale(subtitleScale)
                            .clickable { isSubtitleClicked = !isSubtitleClicked },
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = null
                        },
                        label = { Text(stringResource(Res.string.login_email_label)) },
                        singleLine = true,
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next,
                            ),
                        keyboardActions =
                            KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                            ),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text(stringResource(Res.string.login_password_label)) },
                        singleLine = true,
                        visualTransformation =
                            if (isPasswordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                        keyboardActions =
                            KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    handleSubmit()
                                },
                            ),
                        trailingIcon = {
                            TextButton(
                                onClick = { isPasswordVisible = !isPasswordVisible },
                                enabled = !isLoading,
                                contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
                            ) {
                                Text(
                                    text = if (isPasswordVisible) hidePasswordText else showPasswordText,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { handleSubmit() },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text = if (isLoginMode) signInBtnText else createAccountTitle,
                                style =
                                    MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TextButton(
                        onClick = { isLoginMode = !isLoginMode },
                        enabled = !isLoading,
                    ) {
                        Text(
                            text = if (isLoginMode) toggleToRegisterText else toggleToLoginText,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    val linkStyle =
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                        )
                    val termsStart = termsAgreementText.indexOf(termsLinkText)
                    val privacyStart = termsAgreementText.indexOf(privacyLinkText)
                    val termsText =
                        buildAnnotatedString {
                            append(termsAgreementText)
                            addStringAnnotation(
                                tag = "terms",
                                annotation = "terms",
                                start = termsStart,
                                end = termsStart + termsLinkText.length,
                            )
                            addStyle(linkStyle, termsStart, termsStart + termsLinkText.length)
                            addStringAnnotation(
                                tag = "privacy",
                                annotation = "privacy",
                                start = privacyStart,
                                end = privacyStart + privacyLinkText.length,
                            )
                            addStyle(linkStyle, privacyStart, privacyStart + privacyLinkText.length)
                        }

                    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

                    BasicText(
                        text = termsText,
                        style =
                            MaterialTheme.typography.bodySmall.copy(
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            ),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .pointerInput(isLoading) {
                                    if (!isLoading) {
                                        detectTapGestures { offsetPosition ->
                                            textLayoutResult?.let { layoutResult ->
                                                val offset =
                                                    layoutResult.getOffsetForPosition(offsetPosition)
                                                termsText
                                                    .getStringAnnotations(
                                                        start = offset,
                                                        end = offset,
                                                    ).firstOrNull()
                                                    ?.let { annotation ->
                                                        when (annotation.tag) {
                                                            "terms" ->
                                                                onWebUrlClick(
                                                                    "https://data.androidplay.in/wfy/terms-and-conditions",
                                                                )
                                                            "privacy" ->
                                                                onWebUrlClick(
                                                                    "https://data.androidplay.in/wfy/privacy-policy",
                                                                )
                                                        }
                                                    }
                                            }
                                        }
                                    }
                                },
                        onTextLayout = { textLayoutResult = it },
                    )
                }
            }
        }
    }
}
