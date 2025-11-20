package so.clix.notification

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import so.clix.core.ClixNotification
import so.clix.models.ClixPushNotificationPayload
import so.clix.utils.logging.ClixLogger

/**
 * NotificationTappedActivity is the manifest entry point for notification taps.
 *
 * It simply parses the intent extras and hands control to [ClixNotification], which handles
 * tracking, developer callbacks, and optional landing navigation.
 */
class NotificationTappedActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ClixLogger.debug("NotificationTappedActivity onCreate called")
        ClixLogger.debug("Intent received in onCreate: $intent")
        handleIntent(intent)
        finish()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        ClixLogger.debug("NotificationOpenedActivity onNewIntent called")
        ClixLogger.debug("Intent received in onNewIntent: $intent")
        intent?.let { handleIntent(it) }
        finish()
    }

    private fun handleIntent(intent: Intent) {
        ClixLogger.debug("Handling intent in NotificationTappedActivity: $intent")

        val messageId = intent.getStringExtra("messageId")
        val landingUrl = intent.getStringExtra("landingUrl")
        val userJourneyId = intent.getStringExtra("userJourneyId")
        val userJourneyNodeId = intent.getStringExtra("userJourneyNodeId")
        val autoHandleLandingURL = intent.getBooleanExtra("autoHandleLandingURL", true)
        val notificationDataJson = intent.getStringExtra(NOTIFICATION_DATA_EXTRA)

        if (messageId == null) {
            ClixLogger.warn("messageId is null in intent extras")
            return
        }
        ClixLogger.debug("Extracted messageId: $messageId")

        ClixNotification.handleNotificationTapped(
            context = this,
            payload =
                ClixPushNotificationPayload(
                    title = "",
                    body = "",
                    messageId = messageId,
                    userJourneyId = userJourneyId,
                    userJourneyNodeId = userJourneyNodeId,
                    landingUrl = landingUrl,
                    notificationData =
                        notificationDataJson?.let { deserializeNotificationData(it) } ?: emptyMap(),
                    autoOpenFallback = autoHandleLandingURL,
                ),
        )
    }

    private fun deserializeNotificationData(json: String): Map<String, Any?> {
        return try {
            val jsonObject = JSONObject(json)
            buildMap { jsonObject.keys().forEach { key -> put(key, jsonObject.get(key)) } }
        } catch (e: Exception) {
            ClixLogger.error("Failed to deserialize notification data", e)
            emptyMap()
        }
    }

    /** Holds intent extra keys used by this activity. */
    companion object {
        const val NOTIFICATION_DATA_EXTRA = "clix_notification_data"
    }
}
