package bose.ankush.storage.api

import kotlinx.coroutines.flow.Flow

interface PremiumStorage {
    fun getPremiumStatusFlow(): Flow<PremiumStatus>

    suspend fun savePremiumStatus(
        isPremium: Boolean,
        expiryMillis: Long?,
    )
}

data class PremiumStatus(
    val isPremium: Boolean = false,
    val premiumExpiry: Long? = null,
)
