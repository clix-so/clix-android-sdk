package so.clix.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ClixPushNotificationPayload(
    val title: String,
    val body: String,
    @SerialName("message_id") val messageId: String,
    @SerialName("user_journey_id") val userJourneyId: String? = null,
    @SerialName("user_journey_node_id") val userJourneyNodeId: String? = null,
    @SerialName("landing_url") val landingUrl: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
)
