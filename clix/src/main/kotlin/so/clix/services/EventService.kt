package so.clix.services

import kotlinx.serialization.json.JsonPrimitive
import so.clix.core.Clix

internal class EventService {
    private val eventAPIService = EventAPIService()

    suspend fun trackEvent(
        name: String,
        properties: Map<String, Any?> = emptyMap(),
        messageId: String? = null,
    ) {
        val customProperty =
            properties.mapValues { (_, value) ->
                when (value) {
                    is Boolean -> JsonPrimitive(value)
                    is Number -> JsonPrimitive(value)
                    is String -> JsonPrimitive(value)
                    else -> JsonPrimitive(value?.toString())
                }
            }

        val eventProperty = EventProperty(messageId = messageId, customProperties = customProperty)

        eventAPIService.trackEvents(
            listOf(EventForRequest(Clix.environment.deviceId, name, eventProperty))
        )
    }
}
