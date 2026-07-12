package bose.ankush.analytics

/**
 * Every event the app can log. Each case owns its Firebase event name and parameter map so
 * androidMain/iosMain actuals stay dumb dispatchers — this is the single source of truth that
 * guarantees identical event names/params across platforms.
 */
sealed interface AnalyticsEvent {
    val firebaseName: String
    val params: Map<String, Any?>

    // ---- Standard Firebase funnel events ----

    data class SignUp(val method: String = "email") : AnalyticsEvent {
        override val firebaseName = "sign_up"
        override val params get() = mapOf("method" to method)
    }

    data class Login(val method: String = "email") : AnalyticsEvent {
        override val firebaseName = "login"
        override val params get() = mapOf("method" to method)
    }

    data class Purchase(
        val transactionId: String,
        val value: Double,
        val currency: String,
        val itemName: String,
    ) : AnalyticsEvent {
        override val firebaseName = "purchase"
        override val params
            get() = mapOf(
                "transaction_id" to transactionId,
                "value" to value,
                "currency" to currency,
                "item_name" to itemName,
            )
    }

    data class ScreenView(val screenName: String, val screenClass: String) : AnalyticsEvent {
        override val firebaseName = "screen_view"
        override val params
            get() = mapOf(
                "screen_name" to screenName,
                "screen_class" to screenClass,
            )
    }

    data class Search(val searchTerm: String) : AnalyticsEvent {
        override val firebaseName = "search"
        override val params get() = mapOf("search_term" to searchTerm)
    }

    // ---- Custom funnel/engagement events ----

    data object UpgradePromptShown : AnalyticsEvent {
        override val firebaseName = "upgrade_prompt_shown"
        override val params get() = emptyMap<String, Any?>()
    }

    data class ServiceSelected(val serviceId: String) : AnalyticsEvent {
        override val firebaseName = "service_selected"
        override val params get() = mapOf("service_id" to serviceId)
    }

    data class TierSelected(val serviceId: String, val tierId: String) : AnalyticsEvent {
        override val firebaseName = "tier_selected"
        override val params get() = mapOf("service_id" to serviceId, "tier_id" to tierId)
    }

    data class CheckoutOpened(val value: Double, val currency: String) : AnalyticsEvent {
        override val firebaseName = "checkout_opened"
        override val params get() = mapOf("value" to value, "currency" to currency)
    }

    data class PaymentCancelled(val reason: String?) : AnalyticsEvent {
        override val firebaseName = "payment_cancelled"
        override val params get() = mapOf("reason" to reason)
    }

    data class PaymentFailed(val reason: String?) : AnalyticsEvent {
        override val firebaseName = "payment_failed"
        override val params get() = mapOf("reason" to reason)
    }

    data class NotificationBannerShown(val reason: String) : AnalyticsEvent {
        override val firebaseName = "notification_banner_shown"
        override val params get() = mapOf("reason" to reason)
    }

    data object NotificationBannerDismissed : AnalyticsEvent {
        override val firebaseName = "notification_banner_dismissed"
        override val params get() = emptyMap<String, Any?>()
    }

    data class PermissionResult(
        val permissionType: String,
        val granted: Boolean,
        val permanentlyDeclined: Boolean,
    ) : AnalyticsEvent {
        override val firebaseName = "permission_result"
        override val params
            get() = mapOf(
                "permission_type" to permissionType,
                "granted" to granted,
                "permanently_declined" to permanentlyDeclined,
            )
    }

    data class LocationSaved(val source: String) : AnalyticsEvent {
        override val firebaseName = "location_saved"
        override val params get() = mapOf("source" to source)
    }

    data class LocationSelected(val source: String) : AnalyticsEvent {
        override val firebaseName = "location_selected"
        override val params get() = mapOf("source" to source)
    }

    data object LocationSearchStarted : AnalyticsEvent {
        override val firebaseName = "location_search_started"
        override val params get() = emptyMap<String, Any?>()
    }

    data object Logout : AnalyticsEvent {
        override val firebaseName = "logout"
        override val params get() = emptyMap<String, Any?>()
    }
}
