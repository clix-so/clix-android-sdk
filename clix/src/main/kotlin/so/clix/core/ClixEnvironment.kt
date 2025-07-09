package so.clix.core

import android.content.Context
import android.content.pm.PackageManager
import android.icu.util.TimeZone
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import java.lang.reflect.InvocationTargetException
import kotlinx.serialization.Serializable
import so.clix.models.ClixDevice
import so.clix.utils.logging.ClixLogger

@Serializable
internal class ClixEnvironment
private constructor( // Primary constructor for serialization
    val config: ClixConfig,
    val deviceId: String,
    private var device: ClixDevice, // This property will be serialized/deserialized
) {
    // Internal constructor for object creation by the SDK
    constructor(
        context: Context,
        config: ClixConfig,
        deviceId: String,
        token: String,
    ) : this(
        config = config,
        deviceId = deviceId,
        // Calculate ClixDevice instance here and pass to the primary constructor
        device =
            ClixDevice(
                id = deviceId,
                adId = getAdId(context),
                model = Build.MODEL,
                manufacturer = Build.MANUFACTURER,
                platform = "Android",
                osName = "Android",
                osVersion = Build.VERSION.RELEASE,
                appName = context.packageName,
                appVersion = getAppVersion(context),
                sdkType = "Native",
                sdkVersion = Clix.VERSION,
                pushToken = token,
                pushTokenType = "FCM",
                isPushPermissionGranted = getIsPushPermissionGranted(context),
                localeRegion = context.resources.configuration.locales.get(0).country,
                localeLanguage = context.resources.configuration.locales.get(0).language,
                timezone = TimeZone.getDefault().id,
            ),
    )

    @Synchronized fun getDevice(): ClixDevice = device

    @Synchronized
    fun setDevice(device: ClixDevice) {
        this.device = device
    }

    companion object {
        fun getAppVersion(context: Context): String? {
            try {
                return context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (e: PackageManager.NameNotFoundException) {
                ClixLogger.debug(message = e.localizedMessage)
                return null
            }
        }

        fun getAdId(context: Context): String? {
            try {
                val client =
                    Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient")
                val getAdvertisingInfo =
                    client.getMethod("getAdvertisingIdInfo", Context::class.java)
                val advertisingInfo = getAdvertisingInfo.invoke(null, context)
                val getId = advertisingInfo.javaClass.getMethod("getId")
                return getId.invoke(advertisingInfo) as String
            } catch (_: ClassNotFoundException) {
                ClixLogger.warn(message = "Google Play Services SDK not found for advertising id!")
            } catch (_: InvocationTargetException) {
                ClixLogger.warn(message = "Google Play Services not available for advertising id")
            } catch (e: Exception) {
                ClixLogger.error(
                    message =
                        "Encountered an error connecting to Google Play Services for advertising id",
                    error = e,
                )
            }
            return null
        }

        fun getIsPushPermissionGranted(context: Context): Boolean {
            return NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }
}
