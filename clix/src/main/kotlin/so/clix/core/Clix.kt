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
import so.clix.services.SessionService
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
object Clix {
    /**
     * Notification management interface for handling push notifications, permissions, and related
     * functionality.
     *
     * Use this to configure notification behavior, register handlers, and manage FCM tokens.
     *
     * @see ClixNotification
     */
    @JvmField val Notification = ClixNotification

    // Private implementation
    private val coroutineContext by lazy { SupervisorJob() }

    private object InitializeLock

    private const val CONFIG_KEY = "clix_config"

    @Volatile private var isInitialized = false

    // Internal services
    internal lateinit var environment: ClixEnvironment
    internal lateinit var storageService: StorageService
    internal lateinit var deviceService: DeviceService
    internal lateinit var eventService: EventService
    internal lateinit var tokenService: TokenService
    internal lateinit var notificationService: NotificationService
    internal var sessionService: SessionService? = null
    internal val coroutineScope = CoroutineScope(coroutineContext)

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
        synchronized(InitializeLock) {
            if (isInitialized) {
                ClixLogger.debug("Clix SDK already initialized, skipping")
                return
            }

            try {
                ClixLogger.setLogLevel(config.logLevel)
                val appContext = context.applicationContext
                require(context === appContext) { "Context must be application context." }
                storageService = StorageService(appContext)
                tokenService = TokenService(storageService)
                deviceService = DeviceService(storageService)
                eventService = EventService()
                notificationService = NotificationService(appContext, storageService, eventService)
                sessionService =
                    SessionService(storageService, eventService, config.sessionTimeoutMs)
                sessionService?.start()

                // Save config for recovery when app is killed
                storageService.set(CONFIG_KEY, config)

                val deviceId = deviceService.getCurrentDeviceId()
                val token = tokenService.getCurrentToken() ?: ""

                this.environment = ClixEnvironment(context, config, deviceId, token)
                isInitialized = true
                ClixLogger.debug(
                    "Clix initialized with environment: ${Json.encodeToString(environment)}"
                )

                coroutineScope.launch(Dispatchers.IO) {
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
                        ClixNotification.handleFcmTokenError(e)
                    }
                }
            } catch (e: Exception) {
                isInitialized = false
                ClixLogger.error("Failed to initialize Clix SDK", e)
            }
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
    @Deprecated("Use reset() instead", ReplaceWith("reset()"))
    @JvmStatic
    suspend fun removeUserId() {
        try {
            deviceService.removeProjectUserId()
        } catch (e: Exception) {
            ClixLogger.error("Failed to remove user ID", e)
        }
    }

    /**
     * Resets all local SDK state including device ID.
     *
     * After calling this method, you must call [initialize] again before using the SDK. Use this
     * when a user logs out and you want to start fresh with a new device identity.
     */
    @JvmStatic
    fun reset() {
        try {
            notificationService.reset()
            storageService.remove("clix_device_id")
            storageService.remove("clix_session_last_activity")
            isInitialized = false
        } catch (e: Exception) {
            ClixLogger.error("Failed to reset", e)
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

    /**
     * Initializes the SDK using stored configuration.
     *
     * This is called internally when a push notification is received and the app process was
     * killed. It attempts to restore SDK state using the previously stored configuration. This
     * matches the iOS SDK pattern of `initialize(projectId:)`.
     *
     * @param context The application context (typically from a Service).
     * @return true if SDK is initialized (either already or successfully restored), false
     *   otherwise.
     */
    internal fun initialize(context: Context): Boolean {
        if (isInitialized) {
            return true
        }

        synchronized(InitializeLock) {
            if (isInitialized) {
                return true
            }

            try {
                val appContext = context.applicationContext
                val storageService = StorageService(appContext)
                val savedConfig = storageService.get<ClixConfig>(CONFIG_KEY)

                if (savedConfig == null) {
                    ClixLogger.warn(
                        "Cannot auto-initialize SDK: no saved configuration found. " +
                            "Ensure Clix.initialize() is called in Application.onCreate()"
                    )
                    return false
                }

                ClixLogger.debug("Auto-initializing SDK from saved configuration")
                initialize(appContext, savedConfig)
                return isInitialized
            } catch (e: Exception) {
                ClixLogger.error("Failed to auto-initialize SDK", e)
                return false
            }
        }
    }
}
