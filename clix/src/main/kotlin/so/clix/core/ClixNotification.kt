package so.clix.core

import kotlinx.coroutines.launch
import so.clix.utils.logging.ClixLogger

/**
 * Notification interface for the Clix SDK.
 *
 * Provides methods for setting up notification handling and requesting notification permissions.
 */
object ClixNotification {
    private var isSetupCalled = false

    /**
     * Setup notification handling with optional auto permission request.
     *
     * This method should be called once in your Application.onCreate() or MainActivity.onCreate().
     * If autoRequestPermission is true, the notification permission dialog will be shown
     * automatically on Android 13+.
     *
     * Example:
     * ```kotlin
     * override fun onCreate(savedInstanceState: Bundle?) {
     *     super.onCreate(savedInstanceState)
     *     Clix.Notification.setup(autoRequestPermission = true)
     * }
     * ```
     *
     * @param autoRequestPermission If true, automatically requests notification permission. Default
     *   is false.
     */
    fun setup(autoRequestPermission: Boolean = false) {
        if (isSetupCalled) {
            ClixLogger.debug("ClixNotification.setup() already called, skipping")
            return
        }
        isSetupCalled = true

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

    /**
     * Request notification permission from the user.
     *
     * This method displays the system permission dialog on Android 13+. For earlier Android
     * versions, it returns true immediately as notification permissions are granted at install
     * time.
     *
     * This method handles app restart scenarios. If the app is killed while the permission dialog
     * is showing, the result will be recovered when the app restarts.
     *
     * Example:
     * ```kotlin
     * lifecycleScope.launch {
     *     val granted = Clix.Notification.requestNotificationPermission()
     *     if (granted) {
     *         // Permission granted
     *     }
     * }
     * ```
     *
     * @return true if permission was granted, false if denied
     */
    suspend fun requestNotificationPermission(): Boolean {
        return try {
            Clix.notificationService.requestNotificationPermission()
        } catch (e: Exception) {
            ClixLogger.error("Failed to request notification permission", e)
            false
        }
    }
}
