package bose.ankush.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UnsplashPhoto(
    val id: String,
    val urls: Urls,
    val links: Links,
    val user: User,
) {
    @Serializable
    data class Urls(
        val raw: String,
        val regular: String,
    )

    @Serializable
    data class Links(
        @SerialName("download_location")
        val downloadLocation: String,
    )

    @Serializable
    data class User(
        val name: String,
        val links: UserLinks,
    ) {
        @Serializable
        data class UserLinks(
            val html: String,
        )
    }
}
