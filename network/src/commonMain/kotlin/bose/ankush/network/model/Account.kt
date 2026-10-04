package bose.ankush.network.model

import kotlinx.serialization.Serializable

/** GET /account. [photoUrl] is null until an upload route exists. */
@Serializable
data class Account(
    val email: String = "",
    val photoUrl: String? = null,
)
