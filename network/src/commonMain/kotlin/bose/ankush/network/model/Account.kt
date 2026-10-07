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
