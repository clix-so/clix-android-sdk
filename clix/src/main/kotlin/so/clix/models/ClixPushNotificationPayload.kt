package so.clix.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal data class ClixPushNotificationPayload(
    val title: String,
    val body: String,
    val messageId: String,
    val userJourneyId: String? = null,
    val userJourneyNodeId: String? = null,
    val landingUrl: String? = null,
    val imageUrl: String? = null,
    @Transient val notificationData: Map<String, Any?> = emptyMap(),
    @Transient val autoOpenFallback: Boolean = true,
)
