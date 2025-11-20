package so.clix.core

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import so.clix.BuildConfig
import so.clix.services.DeviceService
import so.clix.services.EventService
import so.clix.services.NotificationService
import so.clix.services.StorageService
import so.clix.services.TokenService
import so.clix.utils.logging.ClixLogLevel
import so.clix.utils.logging.ClixLogger

/**
 * The main entry point for interacting with the Clix SDK. Provides methods for initialization, user
 * identification, event tracking, and more.
 *
 * All public methods include exception handling to prevent crashes in client applications.
 * Exceptions are logged using ClixLogger.
 */
@Suppress("TooManyFunctions")
object Clix {
    @JvmField
    val Notification = ClixNotification

    private val COROUTINE_CONTEXT by lazy { SupervisorJob() }
    internal val coroutineScope = CoroutineScope(COROUTINE_CONTEXT)

    internal lateinit var environment: ClixEnvironment

    internal lateinit var storageService: StorageService

    internal lateinit var deviceService: DeviceService

    internal lateinit var eventService: EventService

    internal lateinit var tokenService: TokenService

    internal lateinit var notificationService: NotificationService

    internal const val VERSION = BuildConfig.VERSION

    /**
     * Initializes the Clix SDK.
     *
     * This must be called once, typically in your Application's `onCreate` method.
     *
     * @param context The application context.
     * @param config The configuration object for the SDK.
     */
    @JvmStatic
    fun initialize(context: Context, config: ClixConfig) {
        try {
            ClixLogger.setLogLevel(config.logLevel)
            val appContext = context.applicationContext
            require(context == appContext) { "Context must be application context." }
            storageService = StorageService(appContext)
            tokenService = TokenService(storageService)
            deviceService = DeviceService(storageService)
            eventService = EventService()
            notificationService = NotificationService(appContext, storageService, eventService)

            val deviceId = deviceService.getCurrentDeviceId()
            val token = tokenService.getCurrentToken() ?: ""

            this.environment = ClixEnvironment(context, config, deviceId, token)
            ClixLogger.debug(
                "Clix initialized with environment: ${Json.encodeToString(environment)}"
            )

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val finalToken =
                        if (token.isNotEmpty()) {
                            ClixLogger.debug("Using existing token: $token")
                            token
                        } else {
                            ClixLogger.debug("Fetching new FCM token")
                            val newToken = FirebaseMessaging.getInstance().token.await()
                            ClixLogger.debug("New FCM token received: $newToken")
                            tokenService.saveToken(newToken)
                            newToken
                        }

                    ClixLogger.debug("Upserting token during initialization: $finalToken")
                    deviceService.upsertToken(finalToken)
                } catch (e: Exception) {
                    ClixLogger.error("Failed to fetch or upsert FCM token", e)
                }
            }
        } catch (e: Exception) {
            ClixLogger.error("Failed to initialize Clix SDK", e)
        }
    }

    /**
     * Sets the user ID for the current user.
     *
     * @param userId The unique identifier for the user.
     */
    @JvmStatic
    suspend fun setUserId(userId: String) {
        try {
            deviceService.setProjectUserId(userId)
        } catch (e: Exception) {
            ClixLogger.error("Failed to set user ID", e)
        }
    }

    /** Removes the user ID for the current user. */
    @JvmStatic
    suspend fun removeUserId() {
        try {
            deviceService.removeProjectUserId()
        } catch (e: Exception) {
            ClixLogger.error("Failed to remove user ID", e)
        }
    }

    /**
     * Sets a single user property.
     *
     * @param name The property name.
     * @param value The property value.
     */
    @JvmStatic
    suspend fun setUserProperty(name: String, value: Any) {
        try {
            deviceService.updateUserProperties(mapOf(name to value))
        } catch (e: Exception) {
            ClixLogger.error("Failed to set user property: $name", e)
        }
    }

    /**
     * Sets multiple user properties.
     *
     * @param properties A map of property names and values.
     */
    @JvmStatic
    suspend fun setUserProperties(properties: Map<String, Any>) {
        try {
            deviceService.updateUserProperties(properties)
        } catch (e: Exception) {
            ClixLogger.error("Failed to set user properties", e)
        }
    }

    /**
     * Removes a single user property.
     *
     * @param name The property name to remove.
     */
    @JvmStatic
    suspend fun removeUserProperty(name: String) {
        try {
            deviceService.removeUserProperties(listOf(name))
        } catch (e: Exception) {
            ClixLogger.error("Failed to remove user property: $name", e)
        }
    }

    /**
     * Removes multiple user properties.
     *
     * @param names The list of property names to remove.
     */
    @JvmStatic
    suspend fun removeUserProperties(names: List<String>) {
        try {
            deviceService.removeUserProperties(names)
        } catch (e: Exception) {
            ClixLogger.error("Failed to remove user properties", e)
        }
    }

    /**
     * Tracks a custom event.
     *
     * @param name The name of the event.
     * @param properties Optional properties associated with the event.
     */
    @JvmStatic
    suspend fun trackEvent(name: String, properties: Map<String, Any?> = emptyMap()) {
        try {
            eventService.trackEvent(name, properties)
        } catch (e: Exception) {
            ClixLogger.error("Failed to track event: $name", e)
        }
    }

    /**
     * Sets the logging level for the SDK.
     *
     * @param level The logging level to set.
     */
    @JvmStatic
    fun setLogLevel(level: ClixLogLevel) {
        try {
            ClixLogger.setLogLevel(level)
        } catch (e: Exception) {
            // Use direct Android logging since ClixLogger might fail
            android.util.Log.e("Clix", "Failed to set log level", e)
        }
    }

    /**
     * Gets the device ID for the current device.
     *
     * @return The device ID as a String.
     */
    @JvmStatic
    fun getDeviceId(): String {
        try {
            return deviceService.getCurrentDeviceId()
        } catch (e: Exception) {
            ClixLogger.error("Failed to get device ID", e)
            return ""
        }
    }
}
