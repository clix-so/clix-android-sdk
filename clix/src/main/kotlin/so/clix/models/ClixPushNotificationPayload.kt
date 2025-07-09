package so.clix.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ClixPushNotificationPayload(
    val title: String,
    val body: String,
    @SerialName("message_id") val messageId: String,
    @SerialName("landing_url") val landingUrl: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
)
