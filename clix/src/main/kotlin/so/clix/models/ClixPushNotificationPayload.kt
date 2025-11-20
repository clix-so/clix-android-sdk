package so.clix.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import org.json.JSONObject
import so.clix.utils.logging.ClixLogger

@Serializable
internal data class ClixPushNotificationPayload(
    val messageId: String,
    val title: String? = null,
    val body: String? = null,
    val userJourneyId: String? = null,
    val userJourneyNodeId: String? = null,
    val imageUrl: String? = null,
    val landingUrl: String? = null,
) {
    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            namingStrategy = JsonNamingStrategy.SnakeCase
        }

        fun decode(notificationData: Map<String, Any?>): ClixPushNotificationPayload? {
            val clixValue = notificationData["clix"]
            if (clixValue == null) {
                ClixLogger.debug("No 'clix' entry found in notification data")
                return null
            }

            val payloadJson =
                when (clixValue) {
                    is String -> clixValue
                    is Map<*, *> -> runCatching { JSONObject(clixValue).toString() }.getOrNull()
                    else -> null
                }

            if (payloadJson.isNullOrEmpty()) {
                ClixLogger.debug("Unable to convert 'clix' entry to JSON string")
                return null
            }

            return try {
                json.decodeFromString<ClixPushNotificationPayload>(payloadJson)
            } catch (e: Exception) {
                ClixLogger.error("Failed to decode Clix payload", e)
                null
            }
        }
    }
}
