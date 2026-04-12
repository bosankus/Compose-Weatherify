package bose.ankush.weatherify.payment.store

import bose.ankush.payment.domain.store.PremiumStatus
import bose.ankush.payment.domain.store.PremiumStore
import bose.ankush.weatherify.domain.preference.PreferenceManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Bridges [PreferenceManager] (Hilt-managed) into the [PremiumStore] interface
 * consumed by [bose.ankush.payment.presentation.PaymentViewModel] (Koin-managed).
 */
internal class PreferenceManagerPremiumStore(
    private val preferenceManager: PreferenceManager,
) : PremiumStore {

    override fun observePremiumStatus(): Flow<PremiumStatus> =
        preferenceManager.getUserPreferencesFlow().map { prefs ->
            PremiumStatus(
                isPremium = prefs.isPremium,
                expiryMillis = prefs.premiumExpiry,
            )
        }

    override suspend fun savePremiumStatus(isPremium: Boolean, expiryMillis: Long) =
        preferenceManager.savePremiumStatus(isPremium, expiryMillis)
}
