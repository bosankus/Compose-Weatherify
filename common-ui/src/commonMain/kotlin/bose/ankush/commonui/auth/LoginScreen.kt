package bose.ankush.commonui.auth

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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

// Multiplatform-safe email regex (replaces android.util.Patterns)
private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

/**
 * Login Screen composable that displays a login form with email and password fields,
 * login/register toggle, and terms & conditions link.
 *
 * CMP-compatible: works on Android and iOS via Compose Multiplatform.
 *
 * @param onLoginClick Callback when the login button is clicked
 * @param onRegisterClick Callback when the register button is clicked
 * @param onWebUrlClick Callback when a web URL (terms/privacy) link is clicked
 * @param isLoading Whether the screen is in loading state
 */
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

    val isEmailValid = { input: String -> EMAIL_REGEX.matches(input) }
    val isPasswordValid = { input: String -> input.length >= 6 }

    val validateInputs = {
        when {
            email.isBlank() -> {
                errorMessage = "Email cannot be empty"
                false
            }
            !isEmailValid(email) -> {
                errorMessage = "Please enter a valid email address"
                false
            }
            password.isBlank() -> {
                errorMessage = "Password cannot be empty"
                false
            }
            !isPasswordValid(password) -> {
                errorMessage = "Password must be at least 6 characters"
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
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 80.dp),
            ) {
                val titleScale by animateFloatAsState(
                    targetValue = if (isTitleClicked) 1.1f else 1.0f,
                    animationSpec = spring(dampingRatio = 0.4f, stiffness = 300f),
                    label = "titleScale",
                )
                val titleColor = if (isTitleClicked) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.primary
                }

                Text(
                    text = if (isLoginMode) "Welcome Back" else "Create Account",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = titleColor,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .scale(titleScale)
                        .clickable { isTitleClicked = !isTitleClicked },
                )

                val subtitleScale by animateFloatAsState(
                    targetValue = if (isSubtitleClicked) 1.1f else 1.0f,
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = androidx.compose.animation.core.FastOutSlowInEasing,
                    ),
                    label = "subtitleScale",
                )
                val subtitleColor = if (isSubtitleClicked) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                }

                Text(
                    text = if (isLoginMode) "Sign in to continue" else "Join our community",
                    style = MaterialTheme.typography.bodyLarge,
                    color = subtitleColor,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(subtitleScale)
                        .clickable { isSubtitleClicked = !isSubtitleClicked },
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Form
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMessage = null },
                        label = { Text("Email address") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = null },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
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
                                    text = if (isPasswordVisible) "Hide" else "Show",
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
                        modifier = Modifier
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
                                text = if (isLoginMode) "Sign In" else "Create Account",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                    }
                }

                // Footer
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TextButton(
                        onClick = { isLoginMode = !isLoginMode },
                        enabled = !isLoading,
                    ) {
                        Text(
                            text = if (isLoginMode) {
                                "Don't have an account? Register"
                            } else {
                                "Already registered? Login"
                            },
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    val termsText = buildAnnotatedString {
                        append("By continuing, you agree to our ")
                        pushStringAnnotation(tag = "terms", annotation = "terms")
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline,
                            ),
                        ) { append("Terms & Conditions") }
                        pop()
                        append(" & ")
                        pushStringAnnotation(tag = "privacy", annotation = "privacy")
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline,
                            ),
                        ) { append("Privacy Policy") }
                        pop()
                    }

                    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

                    BasicText(
                        text = termsText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .pointerInput(isLoading) {
                                if (!isLoading) {
                                    detectTapGestures { offsetPosition ->
                                        textLayoutResult?.let { layoutResult ->
                                            val offset =
                                                layoutResult.getOffsetForPosition(offsetPosition)
                                            termsText.getStringAnnotations(
                                                start = offset,
                                                end = offset
                                            )
                                                .firstOrNull()?.let { annotation ->
                                                    when (annotation.tag) {
                                                        "terms" -> onWebUrlClick("https://data.androidplay.in/wfy/terms-and-conditions")
                                                        "privacy" -> onWebUrlClick("https://data.androidplay.in/wfy/privacy-policy")
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
