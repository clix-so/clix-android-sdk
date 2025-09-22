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
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import so.clix.R
import so.clix.models.ClixPushNotificationPayload
import so.clix.notification.NotificationTappedActivity
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
    private val settingsKey = "clix_notification_settings"
    private val lastNotificationKey = "clix_last_notification"

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val name = "Clix Notifications"
        val descriptionText = "Notifications from Clix SDK"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel =
            NotificationChannel(channelId, name, importance).apply { description = descriptionText }
        notificationManager.createNotificationChannel(channel)
    }

    private fun getLauncherIcon(context: Context): Int {
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
                e.printStackTrace()
                null
            }
        }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private suspend fun showNotification(payload: ClixPushNotificationPayload) {
        val launcherIcon = getLauncherIcon(context)

        val intent =
            Intent(context, NotificationTappedActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("messageId", payload.messageId)
                putExtra("userJourneyId", payload.userJourneyId)
                putExtra("userJourneyNodeId", payload.userJourneyNodeId)
                putExtra("landingUrl", payload.landingUrl)
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
                .setSmallIcon(launcherIcon)
                .setContentTitle(payload.title)
                .setContentText(payload.body)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

        val bitmap = loadImage(payload.imageUrl)
        if (bitmap != null) {
            ClixLogger.debug("Applying BigPictureStyle to notification")
            builder.setLargeIcon(bitmap)
        }

        val notification = builder.build()

        val notificationId = System.currentTimeMillis().toInt()
        ClixLogger.debug("Showing notification with ID: $notificationId")
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private suspend fun trackPushNotificationReceivedEvent(
        messageId: String,
        userJourneyId: String?,
        userJourneyNodeId: String?,
    ) {
        eventService.trackEvent(
            name = NotificationEvent.PUSH_NOTIFICATION_RECEIVED.name,
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
            messageId = messageId,
            userJourneyId = userJourneyId,
            userJourneyNodeId = userJourneyNodeId,
        )
    }

    suspend fun handleNotificationReceived(payload: ClixPushNotificationPayload) {
        try {
            if (
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                showNotification(payload)
                trackPushNotificationReceivedEvent(
                    payload.messageId,
                    payload.userJourneyId,
                    payload.userJourneyNodeId,
                )
                ClixLogger.debug("Message received, notification sent to Clix SDK")
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

    suspend fun setNotificationPreferences(
        context: Context,
        enabled: Boolean,
        categories: List<String>? = null,
    ) {
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

    private fun hasNotificationPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    suspend fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            throw ClixError.NotificationPermissionDenied
        }
    }

    fun reset() {
        storageService.remove(settingsKey)
        storageService.remove(lastNotificationKey)
        notificationManager.cancelAll()
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 1000L
        private const val READ_TIMEOUT_MS = 3000L
    }
}
