package bose.ankush.weatherify.common

import android.app.Activity
import android.content.IntentSender
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
fun startInAppUpdate(activity: Activity) {

    val appUpdateManager = AppUpdateManagerFactory.create(activity)

    val appUpdateInfoTask = appUpdateManager.appUpdateInfo

    appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
        if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
        ) {
            try {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.IMMEDIATE,
                    activity,
                    APP_UPDATE_REQ_CODE
                )

            } catch (exception: IntentSender.SendIntentException) {
            }
        }
    }

}