package bose.ankush.weatherify.presentation

import android.annotation.SuppressLint
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.weatherify.data.preference.PreferenceManager
import com.google.android.gms.location.FusedLocationProviderClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val client: FusedLocationProviderClient,
    private val preferenceManager: PreferenceManager,
) : ViewModel() {

    var permissionDialogQueue = mutableStateListOf<String>()
        private set

    fun dismissDialog() {
        permissionDialogQueue.removeFirst()
    }

    fun onPermissionResult(
        permission: String,
        isGranted: Boolean,
    ) {
        if (!isGranted && !permissionDialogQueue.contains(permission)) {
            permissionDialogQueue.add(permission)
        } else {
            fetchAndSaveLocationCoordinates()
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchAndSaveLocationCoordinates() {
        client.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    viewModelScope.launch {
                        val coordinates = Pair(
                            first = location.latitude,
                            second = location.longitude
                        )
                        preferenceManager.saveLocationPreferences(coordinates)
                    }
                }
            }
            .addOnFailureListener { exception ->
                exception.message
            }
    }
}