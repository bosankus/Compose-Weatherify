package bose.ankush.storage.api

import kotlinx.coroutines.flow.Flow

/**
 * Shared premium-status persistence API.
 *
 * Owned by `:storage` so features that only need “is premium?” (e.g. finder)
 * do not depend on `:feature:payment`. Purchase/order/verify UI stays in payment.
 */
interface PremiumStorage {
    fun observePremiumStatus(): Flow<PremiumStatus>

    suspend fun savePremiumStatus(
        isPremium: Boolean,
        expiryMillis: Long?,
    )
}

data class PremiumStatus(
    val isPremium: Boolean = false,
    val expiryMillis: Long? = null,
)
