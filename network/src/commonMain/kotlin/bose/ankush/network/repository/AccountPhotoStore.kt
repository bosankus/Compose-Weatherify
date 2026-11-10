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

    /**
     * [photoUrl] is the account's only photo now (null: no photo). Any other photo cached before,
     * in this run or an earlier one, is deleted. A re-signed URL for the same photo keeps it.
     */
    fun keepOnly(photoUrl: String?)

    /** Everything, on logout. */
    fun clear()
}
