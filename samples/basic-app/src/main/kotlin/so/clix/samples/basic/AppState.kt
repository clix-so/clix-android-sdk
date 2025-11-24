package so.clix.samples.basic

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppState {
    private const val TAG = "AppState"

    private val deviceIdFlow = MutableStateFlow("Loading...")
    val deviceId: StateFlow<String> = deviceIdFlow.asStateFlow()

    private val fcmTokenFlow = MutableStateFlow("Loading...")
    val fcmToken: StateFlow<String> = fcmTokenFlow.asStateFlow()

    fun updateDeviceId(deviceId: String?) {
        val value = deviceId ?: "Not available"
        Log.d(TAG, "updateDeviceId: $value")
        deviceIdFlow.value = value
    }

    fun updateFCMToken(token: String?) {
        val value = token ?: "Not available"
        Log.d(TAG, "updateFCMToken: $value")
        fcmTokenFlow.value = value
    }
}
