package bose.ankush.weatherify.base.language

import android.content.res.Resources
import androidx.core.os.ConfigurationCompat
import java.util.Locale

class DeviceService {

    /**
     * Returns the preferred device locale based on the current configuration.
     * @return The preferred device locale based on the current configuration.
     */
    fun getDeviceLocale(): Locale? =
        ConfigurationCompat.getLocales(Resources.getSystem().configuration)[0]
}