package so.clix.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import kotlin.jvm.Volatile
import kotlinx.coroutines.launch
import so.clix.models.ClixPushNotificationPayload
import so.clix.utils.logging.ClixLogger

/**
 * Notification interface for the Clix SDK.
 *
 * Provides a central hub for configuring notification behavior (permission prompting, tap handling,
 * landing routing) so apps no longer need to subclass Firebase services for basic overrides.
 */
object ClixNotification {
    @Volatile private var isConfigureCalled = false
    private val configureLock = Any()

    @Volatile private var autoHandleLandingURL: Boolean = true

    @Volatile private var messageHandler: (suspend (Map<String, Any?>) -> Boolean)? = null

    @Volatile private var backgroundMessageHandler: ((Map<String, Any?>) -> Unit)? = null

    @Volatile private var openedHandler: ((Map<String, Any?>) -> Unit)? = null

    internal data class NotificationTapPayload(
        val notificationData: Map<String, Any?>,
        val messageId: String,
        val userJourneyId: String?,
        val userJourneyNodeId: String?,
        val landingUrl: String?,
        val autoOpenFallback: Boolean,
    )

    /**
     * Configure push notification handling with optional settings.
     *
     * This should be called once, typically inside Application.onCreate(). When
     * [autoRequestPermission] is true the POST_NOTIFICATIONS prompt will automatically appear on
     * Android 13+ devices.
     *
     * @param autoRequestPermission Whether to automatically request notification permission
     * @param autoHandleLandingURL Whether to automatically open landing URLs when notifications are
     *   tapped
     */
    fun configure(autoRequestPermission: Boolean = false, autoHandleLandingURL: Boolean = true) {
        synchronized(configureLock) {
            if (isConfigureCalled) {
                ClixLogger.debug("ClixNotification.configure() already called, skipping")
                return
            }
            isConfigureCalled = true
        }

        ClixLogger.debug(
            "ClixNotification.configure(autoRequestPermission: $autoRequestPermission, " +
                "autoHandleLandingURL: $autoHandleLandingURL)"
        )

        this.autoHandleLandingURL = autoHandleLandingURL

        if (autoRequestPermission) {
            Clix.coroutineScope.launch {
                try {
                    val granted = requestPermission()
                    setPermissionGranted(granted)
                    ClixLogger.debug("Auto notification permission result: $granted")
                } catch (e: Exception) {
                    ClixLogger.error("Failed to auto request notification permission", e)
                }
            }
        }
    }

    /** Register handler for foreground messages. */
    fun onMessage(handler: (suspend (Map<String, Any?>) -> Boolean)?) {
        messageHandler = handler
    }

    /** Register handler for background messages. */
    fun onBackgroundMessage(handler: ((Map<String, Any?>) -> Unit)?) {
        backgroundMessageHandler = handler
    }

    /** Register handler for notification taps. */
    fun onNotificationOpened(handler: ((Map<String, Any?>) -> Unit)?) {
        openedHandler = handler
    }

    /** Returns the current FCM token. */
    fun getToken(): String? {
        return try {
            Clix.tokenService.getCurrentToken()
        } catch (e: Exception) {
            ClixLogger.error("Failed to get token", e)
            null
        }
    }

    /** Deletes the FCM token. */
    suspend fun deleteToken() {
        try {
            Clix.tokenService.clearTokens()
            Clix.deviceService.upsertToken("")
            ClixLogger.debug("FCM token deleted successfully")
        } catch (e: Exception) {
            ClixLogger.error("Failed to delete token", e)
        }
    }

    /** Requests notification permissions. */
    suspend fun requestPermission(): Boolean {
        return try {
            Clix.notificationService.requestNotificationPermission()
        } catch (e: Exception) {
            ClixLogger.error("Failed to request notification permission", e)
            false
        }
    }

    /** Returns the current permission status. */
    fun getPermissionStatus(): Boolean {
        return try {
            Clix.notificationService.getPermissionStatus()
        } catch (e: Exception) {
            ClixLogger.error("Failed to get permission status", e)
            false
        }
    }

    /** Updates the permission status on the server. */
    suspend fun setPermissionGranted(isGranted: Boolean) {
        try {
            Clix.deviceService.upsertIsPushPermissionGranted(isGranted)
        } catch (e: Exception) {
            ClixLogger.error("Failed to set permission granted", e)
        }
    }

    internal suspend fun handleIncomingPayload(
        notificationData: Map<String, Any?>,
        payload: ClixPushNotificationPayload,
    ) {
        val handler = messageHandler
        if (handler != null) {
            val shouldDisplay =
                try {
                    handler(notificationData)
                } catch (e: Exception) {
                    ClixLogger.error("Message handler failed", e)
                    true
                }
            if (!shouldDisplay) {
                ClixLogger.debug("Message handler suppressed payload ${payload.messageId}")
                return
            }
        }

        Clix.notificationService.handleNotificationReceived(
            payload = payload,
            notificationData = notificationData,
            autoOpenLandingOnTap = autoHandleLandingURL,
        )
    }

    internal fun handleNotificationTapped(context: Context, payload: NotificationTapPayload) {
        openedHandler?.let { handler ->
            try {
                handler(payload.notificationData)
            } catch (e: Exception) {
                ClixLogger.error("Notification opened handler failed", e)
            }
        }

        Clix.coroutineScope.launch {
            try {
                Clix.notificationService.handleNotificationTapped(
                    payload.messageId,
                    payload.userJourneyId,
                    payload.userJourneyNodeId,
                )
            } catch (e: Exception) {
                ClixLogger.error("Failed to process notification tapped event", e)
            }
        }

        val shouldAutoOpen = autoHandleLandingURL && payload.autoOpenFallback
        if (shouldAutoOpen) {
            openLandingURLIfPresent(context, payload.landingUrl)
        } else {
            ClixLogger.debug(
                "Auto open disabled, skipping landing navigation for ${payload.messageId}"
            )
        }
    }

    private fun openLandingURLIfPresent(context: Context, landingUrl: String?): Boolean {
        val intent = createLandingIntent(context, landingUrl)
        if (intent == null) {
            ClixLogger.warn("Unable to resolve landing destination for URL: $landingUrl")
            return false
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {
            context.startActivity(intent)
            ClixLogger.debug("Launched landing destination for URL: $landingUrl")
            true
        } catch (e: Exception) {
            ClixLogger.error("Failed to launch landing destination", e)
            false
        }
    }

    private fun createLandingIntent(context: Context, landingUrl: String?): Intent? {
        val uri: Uri? = landingUrl?.trim()?.takeIf { it.isNotEmpty() }?.toUri()

        return if (uri != null) {
            Intent(Intent.ACTION_VIEW, uri).apply { addCategory(Intent.CATEGORY_BROWSABLE) }
        } else {
            context.packageManager.getLaunchIntentForPackage(context.packageName)
        }
    }
}
