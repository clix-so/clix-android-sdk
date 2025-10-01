package so.clix.services

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import kotlinx.serialization.json.JsonPrimitive
import so.clix.core.Clix

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
                        convertToInstant(value)?.let { instant ->
                            JsonPrimitive(formatInstantToIso8601(instant))
                        } ?: JsonPrimitive(value?.toString())
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

    private fun convertToInstant(value: Any?): Instant? {
        return when (value) {
            is Date -> value.toInstant()
            is Calendar -> value.toInstant()
            is ZonedDateTime -> value.toInstant()
            is LocalDateTime -> value.atZone(ZoneId.systemDefault()).toInstant()
            is LocalDate -> value.atStartOfDay(ZoneId.systemDefault()).toInstant()
            is Instant -> value
            else -> null
        }
    }

    private fun formatInstantToIso8601(instant: Instant): String {
        return instant.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }
}
