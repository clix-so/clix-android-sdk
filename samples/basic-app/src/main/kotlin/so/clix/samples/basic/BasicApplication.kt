package so.clix.samples.basic

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import so.clix.core.Clix
import so.clix.core.ClixConfig
import so.clix.utils.logging.ClixLogLevel

class BasicApplication : Application() {
    // Create a companion object to hold the UserPreferences instance
    companion object {
        lateinit var userPreferences: UserPreferences
            private set
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize UserPreferences
        userPreferences = UserPreferences(this)
        userPreferences.saveProjectId("")
        userPreferences.saveApiKey("")
        Clix.initialize(
            this,
            ClixConfig(
                projectId = userPreferences.getProjectId()!!,
                apiKey = userPreferences.getApiKey()!!,
                logLevel = ClixLogLevel.DEBUG,
            ),
        )

        // Load stored user ID and set it if available
        val storedUserId = userPreferences.getUserId()
        if (!storedUserId.isNullOrBlank()) {
            CoroutineScope(Dispatchers.IO).launch { Clix.setUserId(storedUserId) }
        }
    }
}
