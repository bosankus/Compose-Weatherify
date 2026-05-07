package bose.ankush.payment.domain.store

import kotlinx.coroutines.flow.Flow

/**
 * Interface for managing and observing the premium subscription status of the user.
 */
interface PremiumStore {
    /**
     * Returns a [Flow] that emits the current [PremiumStatus].
     *
     * @return A flow of premium status updates.
     */
    fun observePremiumStatus(): Flow<PremiumStatus>

    /**
     * Persists the user's premium subscription status.
     *
     * @param isPremium True if the user has an active premium subscription, false otherwise.
     * @param expiryMillis The expiration time of the premium subscription in milliseconds, or null if not applicable.
     */
    suspend fun savePremiumStatus(
        isPremium: Boolean,
        expiryMillis: Long?,
    )
}

/**
 * Data class representing the premium subscription state.
 *
 * @property isPremium Indicates whether the user currently has a premium subscription.
 * @property expiryMillis The timestamp in milliseconds when the premium subscription expires,
 * or null if not applicable.
 */
data class PremiumStatus(
    val isPremium: Boolean,
    val expiryMillis: Long?,
)
