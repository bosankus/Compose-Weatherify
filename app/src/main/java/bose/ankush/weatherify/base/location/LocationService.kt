package bose.ankush.weatherify.base.location

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import bose.ankush.weatherify.R
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach

class LocationService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: LocationClient

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        locationClient = DeviceLocationClient(
            applicationContext,
            LocationServices.getFusedLocationProviderClient(applicationContext)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun start() {
        val notification = NotificationCompat.Builder(
            this,
            NOTIFICATION_CHANNEL_ID
        )
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText("Location: null")
            .setSmallIcon(R.drawable.ic_profile)
            .setOngoing(true)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        locationClient
            .getLocationUpdates(interval = 1000L)
            .catch { exception -> println(exception.printStackTrace()) }
            .onEach { location ->
                val lat = location.latitude.toString().take(4)
                val long = location.longitude.toString().take(4)
                val updatedNotification = notification.setContentText("Location: $lat, $long")

                notificationManager.notify(
                    NOTIFICATION_ID,
                    updatedNotification.build()
                )
            }

        startForeground(
            NOTIFICATION_ID,
            notification.build()
        )
        locationClient.getLocationUpdates(interval = 1000)
    }

    private fun stop() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"

        const val NOTIFICATION_ID = 1
        const val NOTIFICATION_NAME = "Location"
        const val NOTIFICATION_CHANNEL_ID = "location"
        const val NOTIFICATION_TITLE = "Tracking location"
    }
}