package so.clix.notification

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import so.clix.core.Clix
import so.clix.utils.logging.ClixLogger

/**
 * NotificationTappedActivity handles interactions triggered by notifications.
 *
 * This activity is primarily responsible for processing notification intents, determining the
 * appropriate action or destination, and handling navigation based on provided data (e.g.,
 * `messageId` and `landingUrl`).
 *
 * Features:
 * - Handles intents received when a notification is tapped.
 * - Extracts and processes data such as `messageId` and `landingUrl` from the intent extras.
 * - Resolves target destinations, such as opening a URL in a browser or launching the app.
 * - Logs debugging information throughout the lifecycle for better traceability.
 *
 * Flow:
 * 1. When the activity is created (`onCreate`) or a new intent is received (`onNewIntent`), it
 *    invokes the `handleIntent` function.
 * 2. The `messageId` is passed to a coroutine for notification handling.
 * 3. Based on the `landingUrl`, an intent is created to either open a browser or the app.
 *
 * Note:
 * - Invalid or missing data in the intent extras is logged for diagnostics.
 * - This activity finishes itself after processing the intent.
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

        if (messageId == null) {
            ClixLogger.warn("messageId is null in intent extras")
            return
        }
        ClixLogger.debug("Extracted messageId: $messageId")

        Clix.coroutineScope.launch {
            Clix.notificationService.handleNotificationTapped(
                messageId,
                userJourneyId,
                userJourneyNodeId,
            )
        }

        try {
            val destinationIntent = createIntentToOpenUrlOrApp(landingUrl)
            ClixLogger.debug("Resolved destinationIntent: $destinationIntent")
            if (destinationIntent != null) {
                ClixLogger.debug("Starting activity with intent: $destinationIntent")
                startActivity(destinationIntent)
            } else {
                ClixLogger.warn("destinationIntent was null, cannot launch")
            }
        } catch (e: Exception) {
            ClixLogger.error("Failed to open URL or launch app", e)
        }
    }

    private fun createIntentToOpenUrlOrApp(landingUrl: String?): Intent? {
        val uri =
            landingUrl
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.toUri()
                ?.also { ClixLogger.debug("Parsed landing URL: $it") }

        return if (uri != null) {
            openURLInBrowserIntent(uri).also { ClixLogger.debug("Created browser intent: $it") }
        } else {
            packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ClixLogger.debug("Created launch intent for package: $this")
            }
        }
    }

    private fun openURLInBrowserIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
