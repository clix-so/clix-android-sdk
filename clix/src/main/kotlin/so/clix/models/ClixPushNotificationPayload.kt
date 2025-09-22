package so.clix.models

import kotlinx.serialization.Serializable

@Serializable
internal data class ClixPushNotificationPayload(
    val title: String,
    val body: String,
    val messageId: String,
    val landingUrl: String? = null,
    val imageUrl: String? = null,
)
