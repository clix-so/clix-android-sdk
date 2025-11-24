package so.clix.samples.basic

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import so.clix.core.Clix
import so.clix.core.ClixConfig

class BasicApplication : Application() {
    companion object {
        private const val PREFS_NAME = "user_preferences"
        private const val KEY_USER_ID = "user_id"

        lateinit var instance: BasicApplication
            private set

        val sharedPreferences: SharedPreferences
            get() = instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun onCreate() {
        super.onCreate()

        instance = this

        Clix.initialize(
            this,
            ClixConfig(
                projectId = ClixConfiguration.PROJECT_ID,
                apiKey = ClixConfiguration.API_KEY,
                endpoint = ClixConfiguration.ENDPOINT,
                logLevel = ClixConfiguration.LOG_LEVEL,
                extraHeaders = ClixConfiguration.EXTRA_HEADERS
            ),
        )

        Clix.Notification.configure(autoRequestPermission = true)

        updateClixValues()

        val storedUserId = sharedPreferences.getString(KEY_USER_ID, null)
        if (!storedUserId.isNullOrBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                Clix.setUserId(storedUserId)
            }
        }
    }

    private fun updateClixValues() {
        CoroutineScope(Dispatchers.Main).launch {
            val deviceId = Clix.getDeviceId()
            val fcmToken = Clix.Notification.getToken()

            AppState.updateDeviceId(deviceId)
            AppState.updateFCMToken(fcmToken)
        }
    }
}
