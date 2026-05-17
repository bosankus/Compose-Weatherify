package bose.ankush.payment.domain.store

import kotlinx.coroutines.flow.Flow

interface PremiumStore {
    fun observePremiumStatus(): Flow<PremiumStatus>

    suspend fun savePremiumStatus(
        isPremium: Boolean,
        expiryMillis: Long?,
    )
}

data class PremiumStatus(
    val isPremium: Boolean,
    val expiryMillis: Long?,
)
