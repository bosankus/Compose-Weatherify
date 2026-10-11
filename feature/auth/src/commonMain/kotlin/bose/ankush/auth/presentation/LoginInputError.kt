package bose.ankush.auth.presentation

/** Why the sign-in form can't be sent yet. Each platform shows its own localized line. */
enum class LoginInputError {
    EMAIL_EMPTY,
    EMAIL_INVALID,
    PASSWORD_EMPTY,
    PASSWORD_SHORT,
}

/** The first problem with the form, in the order the fields appear, or null when it can be sent. */
fun validateLogin(
    email: String,
    password: String,
): LoginInputError? =
    when {
        email.isBlank() -> LoginInputError.EMAIL_EMPTY
        !EMAIL_REGEX.matches(email.trim()) -> LoginInputError.EMAIL_INVALID
        password.isBlank() -> LoginInputError.PASSWORD_EMPTY
        password.length < MIN_PASSWORD_LENGTH -> LoginInputError.PASSWORD_SHORT
        else -> null
    }

private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
private const val MIN_PASSWORD_LENGTH = 6
