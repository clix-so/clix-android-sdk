package so.clix.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import so.clix.core.Clix
import so.clix.models.ClixPushNotificationPayload
import so.clix.utils.logging.ClixLogger

/**
 * Firebase Messaging Service implementation for handling Clix push notifications.
 *
 * This service automatically handles incoming FCM messages, parses Clix-specific data, and updates
 * user properties accordingly. It also logs notification details.
 *
 * To use this, declare it in your `AndroidManifest.xml`:
 * ```xml
 * <service
 *     android:name="so.clix.notification.ClixMessagingService"
 *     android:exported="false">
 *     <intent-filter>
 *         <action android:name="com.google.firebase.MESSAGING_EVENT" />
 *     </intent-filter>
 * </service>
 * ```
 */
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
open class ClixMessagingService : FirebaseMessagingService() {
    private val json = Json { ignoreUnknownKeys = true }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        ClixLogger.debug("Message received $message, from: ${message.from}")
        message.data.isNotEmpty().let { ClixLogger.debug("Message data payload: ${message.data}") }
        message.notification?.let { ClixLogger.debug("Message notification body: ${it.body}") }

        val payload =
            try {
                message.data["clix"]?.let { json.decodeFromString<ClixPushNotificationPayload>(it) }
            } catch (e: Exception) {
                ClixLogger.error("Failed to parse clix payload", e)
                null
            }

        if (payload == null) {
            ClixLogger.error("No valid clix payload found in message data")
            return
        }

        Clix.coroutineScope.launch { Clix.notificationService.handleNotificationReceived(payload) }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        ClixLogger.debug("New token received $token")
        Clix.coroutineScope.launch {
            try {
                Clix.tokenService.saveToken(token)
                Clix.deviceService.upsertToken(token)
            } catch (e: Exception) {
                ClixLogger.error("upsertToken failure:", e)
            }
        }
    }
}
