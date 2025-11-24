package so.clix.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import kotlin.jvm.Volatile
import kotlinx.coroutines.launch
import so.clix.models.ClixPushNotificationPayload
import so.clix.models.NotificationContext
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

    @Volatile private var fcmTokenErrorHandler: ((Exception) -> Unit)? = null

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

    /**
     * Register a handler for foreground messages received when the app is in the foreground.
     *
     * The handler receives notification data as a map and should return true to display the
     * notification or false to suppress it. If no handler is registered or if the handler throws an
     * exception, the notification will be displayed by default.
     *
     * @param handler A suspending function that receives notification data and returns whether to
     *   display the notification. Pass null to unregister the handler.
     */
    fun onMessage(handler: (suspend (Map<String, Any?>) -> Boolean)?) {
        messageHandler = handler
    }

    /**
     * Register a handler for background messages received when the app is in the background or
     * terminated.
     *
     * This handler is invoked when a notification is received while the app is not in the
     * foreground. It allows you to process the notification data before the system displays it.
     *
     * @param handler A function that receives notification data as a map. Pass null to unregister
     *   the handler.
     */
    fun onBackgroundMessage(handler: ((Map<String, Any?>) -> Unit)?) {
        backgroundMessageHandler = handler
    }

    /**
     * Register a handler that is invoked when a user taps on a notification.
     *
     * This handler is called before any automatic landing URL navigation occurs. It receives the
     * notification data as a map, allowing you to perform custom actions or analytics tracking when
     * a notification is opened.
     *
     * @param handler A function that receives notification data as a map. Pass null to unregister
     *   the handler.
     */
    fun onNotificationOpened(handler: ((Map<String, Any?>) -> Unit)?) {
        openedHandler = handler
    }

    /**
     * Register a handler for FCM token errors.
     *
     * This handler is invoked when there is an error fetching or refreshing the Firebase Cloud
     * Messaging (FCM) token. It allows you to handle token-related errors gracefully, such as
     * logging or alerting.
     *
     * @param handler A function that receives the Exception that occurred. Pass null to unregister
     *   the handler.
     */
    fun onFcmTokenError(handler: ((Exception) -> Unit)?) {
        fcmTokenErrorHandler = handler
    }

    /**
     * Returns the current Firebase Cloud Messaging (FCM) token for this device.
     *
     * The FCM token is used to uniquely identify this device for push notifications. This token can
     * be used for testing or debugging purposes.
     *
     * @return The current FCM token as a String, or null if the token is not available or if an
     *   error occurs.
     */
    fun getToken(): String? {
        return try {
            Clix.tokenService.getCurrentToken()
        } catch (e: Exception) {
            ClixLogger.error("Failed to get token", e)
            null
        }
    }

    /**
     * Deletes the current FCM token and notifies the server.
     *
     * This clears the locally stored FCM token and updates the server to indicate that this device
     * no longer has a valid token. After deletion, the device will no longer receive push
     * notifications until a new token is generated and registered.
     *
     * This is a suspending function and must be called from a coroutine or another suspending
     * function.
     */
    suspend fun deleteToken() {
        try {
            Clix.tokenService.clearTokens()
            Clix.deviceService.upsertToken("")
            ClixLogger.debug("FCM token deleted successfully")
        } catch (e: Exception) {
            ClixLogger.error("Failed to delete token", e)
        }
    }

    /**
     * Requests notification permissions from the user.
     *
     * On Android 13 (API level 33) and above, this will display the system permission dialog to
     * request POST_NOTIFICATIONS permission. On earlier Android versions, this will always return
     * true as notification permissions are granted by default.
     *
     * This is a suspending function and must be called from a coroutine or another suspending
     * function.
     *
     * @return true if the permission was granted, false otherwise.
     */
    suspend fun requestPermission(): Boolean {
        return try {
            Clix.notificationService.requestNotificationPermission()
        } catch (e: Exception) {
            ClixLogger.error("Failed to request notification permission", e)
            false
        }
    }

    /**
     * Returns the current notification permission status.
     *
     * This checks whether the app currently has permission to display notifications. On Android 13
     * (API level 33) and above, this checks the POST_NOTIFICATIONS permission. On earlier versions,
     * this always returns true as notification permissions are granted by default.
     *
     * @return true if notification permission is granted, false otherwise.
     */
    fun getPermissionStatus(): Boolean {
        return try {
            Clix.notificationService.getPermissionStatus()
        } catch (e: Exception) {
            ClixLogger.error("Failed to get permission status", e)
            false
        }
    }

    /**
     * Updates the notification permission status on the server.
     *
     * This informs the Clix backend of the current notification permission state for this device.
     * It is typically called automatically after requesting permission, but can be called manually
     * if needed to sync the permission state.
     *
     * This is a suspending function and must be called from a coroutine or another suspending
     * function.
     *
     * @param isGranted true if notification permission is granted, false otherwise.
     */
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
            autoHandleLandingURL = autoHandleLandingURL,
        )
    }

    internal fun handleNotificationTapped(
        context: Context,
        notificationContext: NotificationContext,
        payload: ClixPushNotificationPayload,
    ) {
        openedHandler?.let { handler ->
            try {
                handler(notificationContext.notificationData)
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

        val shouldAutoOpen = autoHandleLandingURL && notificationContext.autoOpenLandingURL
        if (shouldAutoOpen) {
            openLandingURLIfPresent(context, payload.landingUrl)
        } else {
            ClixLogger.debug(
                "Auto open disabled, skipping landing navigation for ${payload.messageId}"
            )
        }
    }

    internal fun handleFcmTokenError(error: Exception) {
        ClixLogger.error("FCM token error", error)
        fcmTokenErrorHandler?.invoke(error)
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
