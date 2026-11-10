package bose.ankush.weatherify.base.common

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import timber.log.Timber

/**
 * Forces an immediate Play update when one is available. Call [checkForUpdate] from onResume: that
 * covers launch, and it also resumes an update the user backed out of, which IMMEDIATE requires.
 *
 * Create it as an activity property, so the result launcher is registered before the activity
 * starts.
 */
class InAppUpdateManager(
    private val activity: ComponentActivity,
) {
    // Lazy: the activity has no context yet when its properties are initialised.
    private val appUpdateManager by lazy { AppUpdateManagerFactory.create(activity) }

    private val updateLauncher =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                Timber.w("In-app update not completed, result code ${result.resultCode}")
            }
        }

    fun checkForUpdate() {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                val availability = info.updateAvailability()
                val resumable =
                    availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                val available =
                    availability == UpdateAvailability.UPDATE_AVAILABLE &&
                            info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                if (resumable || available) {
                    appUpdateManager.startUpdateFlowForResult(
                        info,
                        updateLauncher,
                        AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE),
                    )
                }
            }.addOnFailureListener { Timber.w(it, "Couldn't check for an in-app update") }
    }
}
