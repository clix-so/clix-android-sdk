package so.clix.services

import ClixError
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.json.JSONObject
import so.clix.R
import so.clix.models.ClixPushNotificationPayload
import so.clix.notification.NotificationTappedActivity
import so.clix.notification.PermissionActivity
import so.clix.utils.logging.ClixLogger

@Serializable
internal data class NotificationSettings(
    val enabled: Boolean,
    val categories: List<String>? = null,
    val lastUpdated: Long = System.currentTimeMillis(),
)

internal enum class NotificationEvent {
    PUSH_NOTIFICATION_RECEIVED,
    PUSH_NOTIFICATION_TAPPED,
}

internal class NotificationService(
    private val context: Context,
    private val storageService: StorageService,
    private val eventService: EventService,
) {
    private val notificationManager = NotificationManagerCompat.from(context)
    private val channelId = "clix_notification_channel"
    private val groupKey = "clix_notification_group"
    private val settingsKey = "clix_notification_settings"
    private val lastReceivedMessageIdKey = "clix_last_received_message_id"

    private val launcherIcon: Int by lazy { resolveLauncherIcon() }
    private val accentColor: Int? by lazy { resolveAccentColor() }

    init {
        createNotificationChannel()
    }

    suspend fun handleNotificationReceived(
        payload: ClixPushNotificationPayload,
        notificationData: Map<String, Any?>,
        autoHandleLandingURL: Boolean = true,
    ) {
        try {
            if (hasNotificationPermission(context)) {
                val shouldTrack = recordReceivedMessageId(payload.messageId)
                if (!shouldTrack) {
                    val eventName = NotificationEvent.PUSH_NOTIFICATION_RECEIVED.name
                    ClixLogger.debug(
                        "Skipping duplicate $eventName for messageId: ${payload.messageId}"
                    )
                    return
                }

                try {
                    showNotification(payload, notificationData, autoHandleLandingURL)
                    trackPushNotificationReceivedEvent(
                        payload.messageId,
                        payload.userJourneyId,
                        payload.userJourneyNodeId,
                    )
                    ClixLogger.debug("Message received, notification sent to Clix SDK")
                } catch (e: Exception) {
                    recoverReceivedMessageId(payload.messageId)
                    throw e
                }
            } else {
                ClixLogger.warn("Notification permission not granted, cannot show notification")
            }
        } catch (e: Exception) {
            ClixLogger.error("Failed to handle notification received", e)
        }
    }

    suspend fun handleNotificationTapped(
        messageId: String,
        userJourneyId: String? = null,
        userJourneyNodeId: String? = null,
    ) {
        try {
            trackPushNotificationTappedEvent(messageId, userJourneyId, userJourneyNodeId)
            ClixLogger.debug("Notification tapped, tracking event for messageId: $messageId")
        } catch (e: Exception) {
            ClixLogger.error("Failed to handle notification tapped", e)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun requestNotificationPermission(): Boolean {
        return try {
            if (hasNotificationPermission(context)) {
                ClixLogger.debug("Notification permission already granted")
                return true
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ClixLogger.debug("Requesting notification permission")
                PermissionActivity.requestPermission(context)
            } else {
                ClixLogger.debug(
                    "Notification permission must be managed via system settings (Android < 13)"
                )
                hasNotificationPermission(context)
            }
        } catch (e: Exception) {
            ClixLogger.error("Failed to request notification permission", e)
            false
        }
    }

    fun setNotificationPreferences(enabled: Boolean, categories: List<String>? = null) {
        if (enabled && !hasNotificationPermission(context)) {
            throw ClixError.NotificationPermissionDenied
        }

        val settings =
            NotificationSettings(
                enabled = enabled,
                categories = categories,
                lastUpdated = System.currentTimeMillis(),
            )
        storageService.set(settingsKey, settings)
    }

    fun getPermissionStatus(): Boolean {
        return hasNotificationPermission(context)
    }

    fun reset() {
        storageService.remove(settingsKey)
        storageService.remove(lastReceivedMessageIdKey)
        notificationManager.cancelAll()
    }

    @RequiresPermission(value = Manifest.permission.POST_NOTIFICATIONS, conditional = true)
    private suspend fun showNotification(
        payload: ClixPushNotificationPayload,
        notificationData: Map<String, Any?>,
        autoHandleLandingURL: Boolean = true,
    ) {
        val intent =
            Intent(context, NotificationTappedActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("messageId", payload.messageId)
                putExtra("userJourneyId", payload.userJourneyId)
                putExtra("userJourneyNodeId", payload.userJourneyNodeId)
                putExtra("landingUrl", payload.landingUrl)
                putExtra("autoHandleLandingURL", autoHandleLandingURL)
                notificationData.toJsonString()?.let {
                    putExtra(NotificationTappedActivity.NOTIFICATION_DATA_EXTRA, it)
                }
            }

        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val builder =
            NotificationCompat.Builder(context, channelId)
                .setGroup(groupKey)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
                .setSmallIcon(launcherIcon)
                .setContentTitle(payload.title.orEmpty())
                .setContentText(payload.body.orEmpty())
                .setContentIntent(pendingIntent)
                .setStyle(NotificationCompat.BigTextStyle().bigText(payload.body.orEmpty()))
                .setTicker(payload.body.orEmpty())
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_ALL)

        accentColor?.let { builder.color = it }

        val bitmap = loadImage(payload.imageUrl)
        if (bitmap != null) {
            ClixLogger.debug("Applying BigPictureStyle to notification")
            builder.setLargeIcon(bitmap)
        }

        val notification = builder.build()

        val notificationId = payload.messageId.hashCode()
        ClixLogger.debug(
            "Showing notification with ID: $notificationId for messageId: ${payload.messageId}"
        )
        notificationManager.notify(notificationId, notification)
    }

    private suspend fun trackPushNotificationReceivedEvent(
        messageId: String,
        userJourneyId: String?,
        userJourneyNodeId: String?,
    ) {
        eventService.trackEvent(
            name = NotificationEvent.PUSH_NOTIFICATION_RECEIVED.name,
            sourceType = "CLIX",
            messageId = messageId,
            userJourneyId = userJourneyId,
            userJourneyNodeId = userJourneyNodeId,
        )
    }

    private suspend fun trackPushNotificationTappedEvent(
        messageId: String,
        userJourneyId: String? = null,
        userJourneyNodeId: String? = null,
    ) {
        eventService.trackEvent(
            name = NotificationEvent.PUSH_NOTIFICATION_TAPPED.name,
            sourceType = "CLIX",
            messageId = messageId,
            userJourneyId = userJourneyId,
            userJourneyNodeId = userJourneyNodeId,
        )
    }

    private fun recordReceivedMessageId(messageId: String): Boolean {
        synchronized(this) {
            val previous = storageService.get<String>(lastReceivedMessageIdKey)
            if (previous == messageId) {
                return false
            }
            storageService.set(lastReceivedMessageIdKey, messageId)
            return true
        }
    }

    private fun recoverReceivedMessageId(messageId: String) {
        synchronized(this) {
            val previous = storageService.get<String>(lastReceivedMessageIdKey)
            if (previous == messageId) {
                storageService.remove(lastReceivedMessageIdKey)
            }
        }
    }

    private fun createNotificationChannel() {
        val name = "Clix Notifications"
        val descriptionText = "Notifications from Clix SDK"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel =
            NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
            }
        notificationManager.createNotificationChannel(channel)
    }

    private fun resolveLauncherIcon(): Int {
        val appInfo =
            context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA,
            )

        val launcherIcon = appInfo.icon
        return if (launcherIcon != 0) {
            launcherIcon
        } else {
            R.drawable.baseline_notifications_24
        }
    }

    private fun resolveAccentColor(): Int? {
        return try {
            val typedValue = android.util.TypedValue()
            val theme = context.theme

            if (theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)) {
                return typedValue.data
            }

            null
        } catch (e: Exception) {
            ClixLogger.debug("Failed to get accent color from theme: ${e.message}")
            null
        }
    }

    private suspend fun loadImage(src: String?): Bitmap? =
        withContext(Dispatchers.IO) {
            ClixLogger.debug("Loading image from URL: $src")

            if (src == null) return@withContext null
            try {
                val url = URL(src)
                val connection =
                    url.openConnection().apply {
                        connectTimeout = CONNECT_TIMEOUT_MS.toInt()
                        readTimeout = READ_TIMEOUT_MS.toInt()
                    }
                connection.getInputStream().use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            } catch (e: ConnectException) {
                ClixLogger.warn("Connection error", e)
                null
            } catch (e: SocketTimeoutException) {
                ClixLogger.warn("Read timeout", e)
                null
            } catch (e: Exception) {
                ClixLogger.error("Failed to load image", e)
                null
            }
        }

    @Suppress("TooGenericExceptionCaught")
    private fun Map<String, Any?>.toJsonString(): String? {
        return try {
            JSONObject(this as Map<*, *>).toString()
        } catch (e: Exception) {
            ClixLogger.error("Failed to serialize raw notification data", e)
            null
        }
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 1000L
        private const val READ_TIMEOUT_MS = 3000L

        fun hasNotificationPermission(context: Context): Boolean {
            return NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }
}
