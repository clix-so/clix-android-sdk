package so.clix.services

import kotlinx.serialization.json.JsonPrimitive
import so.clix.core.Clix
import so.clix.utils.ClixDateFormatter

internal class EventService {
    private val eventAPIService = EventAPIService()

    suspend fun trackEvent(
        name: String,
        properties: Map<String, Any?> = emptyMap(),
        messageId: String? = null,
        userJourneyId: String? = null,
        userJourneyNodeId: String? = null,
    ) {
        val customProperty =
            properties.mapValues { (_, value) ->
                when (value) {
                    is Boolean -> JsonPrimitive(value)
                    is Number -> JsonPrimitive(value.toDouble())
                    is String -> JsonPrimitive(value)
                    else ->
                        ClixDateFormatter.format(value)?.let { JsonPrimitive(it) }
                            ?: JsonPrimitive(value?.toString())
                }
            }

        val eventProperty =
            EventProperties(
                messageId = messageId,
                userJourneyId = userJourneyId,
                userJourneyNodeId = userJourneyNodeId,
                customProperties = customProperty,
            )

        eventAPIService.trackEvents(
            listOf(EventForRequest(Clix.environment.deviceId, name, eventProperty))
        )
    }
}
