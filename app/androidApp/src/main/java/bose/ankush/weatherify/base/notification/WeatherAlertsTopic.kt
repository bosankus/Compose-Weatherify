package bose.ankush.weatherify.base.notification

import android.content.Context
import androidx.core.content.edit
import com.google.firebase.messaging.FirebaseMessaging
import timber.log.Timber

/**
 * The weather_alerts topic broadcast. Subscribes once rather than on every launch, and again when
 * the FCM token changes, since topic subscriptions belong to the token.
 */
object WeatherAlertsTopic {
    private const val TOPIC = "weather_alerts"
    private const val PREFS = "fcm_topics"
    private const val KEY_SUBSCRIBED = "weather_alerts_subscribed"

    fun ensureSubscribed(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_SUBSCRIBED, false)) return
        FirebaseMessaging
            .getInstance()
            .subscribeToTopic(TOPIC)
            .addOnSuccessListener {
                prefs.edit { putBoolean(KEY_SUBSCRIBED, true) }
                Timber.d("Subscribed to $TOPIC topic")
            }.addOnFailureListener { Timber.e(it, "Failed to subscribe to $TOPIC topic") }
    }

    /** The token changed, so the old subscription went with it. */
    fun onTokenRotated(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { remove(KEY_SUBSCRIBED) }
        ensureSubscribed(context)
    }
}
