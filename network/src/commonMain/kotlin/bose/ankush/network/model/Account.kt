package bose.ankush.network.model

import kotlinx.serialization.Serializable

/**
 * GET /account. [photoUrl] is a short-lived signed URL, or null when the user has no photo.
 * The storage object key is not part of this model.
 */
@Serializable
data class Account(
    val email: String = "",
    val photoUrl: String? = null,
)

/** Body of POST and DELETE /account/photo: the new signed [photoUrl], or null when removed. */
@Serializable
data class AccountPhoto(
    val photoUrl: String? = null,
)

/** Body of DELETE /account: the current password, re-entered to confirm. */
@Serializable
data class DeleteAccountRequest(
    val password: String,
)

/** `data` of an error envelope. Match on [errorCode], never on the message text. */
@Serializable
data class ApiErrorData(
    val errorCode: String? = null,
)

/**
 * What DELETE /account came back with. Only [Deleted] means the account is gone; every other
 * outcome leaves it (and so the local session and data) in place.
 */
sealed interface DeleteAccountResult {
    /** 200. The account no longer exists. */
    data object Deleted : DeleteAccountResult

    /** 403 with `INVALID_PASSWORD`. Still signed in. */
    data object IncorrectPassword : DeleteAccountResult

    /** 401 that survived a token refresh. The interceptor has already ended the session. */
    data object SessionExpired : DeleteAccountResult

    /** 429. Same limit as login, 10 per minute. */
    data object RateLimited : DeleteAccountResult

    /** 400. The body didn't validate, which is a client bug. */
    data object InvalidRequest : DeleteAccountResult

    /** 5xx, an unexpected status, or no response at all. The account may still exist. */
    data class Failed(
        val cause: Throwable? = null,
    ) : DeleteAccountResult
}
