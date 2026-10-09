package bose.ankush.network.repository

/**
 * Where the image layer keeps the account photo. [AccountRepositoryImpl] calls it before it
 * publishes a change, so a screen that sees the new URL finds the photo already in place.
 */
interface AccountPhotoStore {
    /** The bytes just uploaded for [photoUrl], so showing them needs no download. */
    suspend fun store(
        photoUrl: String,
        bytes: ByteArray,
    )

    /** The photo at [photoUrl] was removed. */
    fun remove(photoUrl: String)

    /** Everything, on logout. */
    fun clear()
}
