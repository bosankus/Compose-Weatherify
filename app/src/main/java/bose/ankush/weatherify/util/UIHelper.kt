package bose.ankush.weatherify.util

import timber.log.Timber

/**
 * Created by Androidplay
 * Author: Ankush
 * On: 4/26/2020, 10:51 AM
 */

/** General ui related extension functions */

object UIHelper {

    /*fun getCurrentTimestamp(): String = Instant.now().toEpochMilli().toString()*/

    fun logMessage(message: String) = Timber.d(message)

    /*fun showToast(context: Context, toastMessage: String) {
        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
    }

    fun showSnack(view: View, message: String) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
    }*/
}