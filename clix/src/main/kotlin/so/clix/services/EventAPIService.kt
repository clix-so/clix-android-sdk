package so.clix.services

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
internal data class EventProperties(
    val messageId: String? = null,
    val userJourneyId: String? = null,
    val userJourneyNodeId: String? = null,
    val customProperties: Map<String, JsonPrimitive>,
)

@Serializable
internal data class EventForRequest(
    val deviceId: String,
    val name: String,
    val properties: EventProperties,
)

@Serializable
internal data class Event(
    val userId: String,
    val deviceId: String,
    val name: String,
    val properties: EventProperties,
)

@Serializable internal data class CreateEventsRequest(val events: List<EventForRequest>)

@Serializable internal data class CreateEventsResponse(val events: List<Event>)

internal class EventAPIService : ClixAPIClient() {
    suspend fun trackEvents(events: List<EventForRequest>) {
        val data = CreateEventsRequest(events = events)
        post<CreateEventsRequest, CreateEventsResponse>(path = "/events", data = data)
    }
}
