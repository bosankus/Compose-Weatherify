package bose.ankush.weatherify.di

import bose.ankush.network.repository.PushTokenProvider
import bose.ankush.network.repository.PushTokenRegistration
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.coroutines.resume

/** This device's FCM token, registered with the account so account changes reach it. */
val appPushKoinModule: Module =
    module {
        single<PushTokenProvider> {
            PushTokenProvider {
                suspendCancellableCoroutine { cont ->
                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                        if (cont.isActive) cont.resume(if (task.isSuccessful) task.result else null)
                    }
                }
            }
        }
        single { PushTokenRegistration(get(), get()) }
    }
