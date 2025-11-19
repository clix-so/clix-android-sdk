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
    @Volatile private var isSetupCalled = false
    private val setupLock = Any()

    @Volatile private var autoOpenLandingOnTap: Boolean = true

    private var willShowHandler: (suspend (Map<String, Any?>) -> Boolean)? = null

    internal data class NotificationTapPayload(
        val notificationData: Map<String, Any?>,
        val messageId: String,
        val userJourneyId: String?,
        val userJourneyNodeId: String?,
        val landingUrl: String?,
        val autoOpenFallback: Boolean,
    )

    /**
     * Setup notification handling with optional auto permission request.
     *
     * This should be called once, typically inside Application.onCreate(). When
     * [autoRequestPermission] is true the POST_NOTIFICATIONS prompt will automatically appear on
     * Android 13+ devices.
     */
    fun setup(autoRequestPermission: Boolean = false) {
        synchronized(setupLock) {
            if (isSetupCalled) {
                ClixLogger.debug("ClixNotification.setup() already called, skipping")
                return
            }
            isSetupCalled = true
        }

        ClixLogger.debug("ClixNotification.setup(autoRequestPermission: $autoRequestPermission)")

        if (autoRequestPermission) {
            Clix.coroutineScope.launch {
                try {
                    val granted = requestNotificationPermission()
                    Clix.setPushPermissionGranted(granted)
                    ClixLogger.debug("Auto notification permission result: $granted")
                } catch (e: Exception) {
                    ClixLogger.error("Failed to auto request notification permission", e)
                }
            }
        }
    }

    /** Globally enable/disable automatic landing URL opening when a notification is tapped. */
    fun setAutoOpenLandingOnTap(enabled: Boolean) {
        autoOpenLandingOnTap = enabled
        ClixLogger.debug("autoOpenLandingOnTap set to $enabled")
    }

    /** Register a handler invoked before the SDK displays a notification in the foreground. */
    fun setNotificationWillShowInForegroundHandler(
        handler: (suspend (Map<String, Any?>) -> Boolean)?
    ) {
        willShowHandler = handler
    }

    /** Register a callback invoked when the user taps a notification. */
    fun setNotificationOpenedHandler(handler: ((Map<String, Any?>) -> Unit)?) {
        openedHandler = handler
    }

    /** Request notification permission from the user. */
    suspend fun requestNotificationPermission(): Boolean {
        return try {
            Clix.notificationService.requestNotificationPermission()
        } catch (e: Exception) {
            ClixLogger.error("Failed to request notification permission", e)
            false
        }
    }

    internal suspend fun handleIncomingPayload(
        notificationData: Map<String, Any?>,
        payload: ClixPushNotificationPayload,
    ) {
        val handler = willShowHandler
        if (handler != null) {
            val shouldDisplay =
                try {
                    handler(notificationData)
                } catch (e: Exception) {
                    ClixLogger.error("Notification will-show handler failed", e)
                    true
                }
            if (!shouldDisplay) {
                ClixLogger.debug(
                    "Notification will-show handler suppressed payload ${payload.messageId}"
                )
                return
            }
        }

        Clix.notificationService.handleNotificationReceived(
            payload = payload,
            notificationData = notificationData,
            autoOpenLandingOnTap = autoOpenLandingOnTap,
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

        val shouldAutoOpen = autoOpenLandingOnTap && payload.autoOpenFallback
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
