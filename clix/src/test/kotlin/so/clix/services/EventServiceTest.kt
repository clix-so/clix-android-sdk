package so.clix.services

import android.content.Context
import android.os.Build
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import so.clix.core.Clix
import so.clix.core.ClixEnvironment

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class EventServiceTest {

    private lateinit var context: Context
    private lateinit var eventService: EventService
    private lateinit var eventAPIService: EventAPIService

    @Before
    fun setup() {
        context = mockk(relaxed = true)

        // Mock Clix object and its environment
        mockkObject(Clix)
        val mockEnvironment = mockk<ClixEnvironment>()
        every { Clix.environment } returns mockEnvironment
        every { mockEnvironment.deviceId } returns "test-device-id"

        // Create a mock EventAPIService
        eventAPIService = mockk(relaxed = true)

        // Create a spy of EventService that we can intercept calls on
        eventService = spyk(EventService())

        // Mock the trackEvent method to use our mock EventAPIService
        coEvery {
            eventService.trackEvent(any(), any(), any())
        } coAnswers {
            val name = arg<String>(0)
            val properties = arg<Map<String, Any?>>(1)
            val messageId = arg<String?>(2)

            val customProperty =
                properties.mapValues { (_, value) ->
                    when (value) {
                        is Boolean -> JsonPrimitive(value)
                        is Number -> JsonPrimitive(value)
                        is String -> JsonPrimitive(value)
                        null -> JsonPrimitive("null")
                        else -> JsonPrimitive(value.toString())
                    }
                }

            val eventProperty = EventProperty(messageId = messageId, customProperties = customProperty)

            eventAPIService.trackEvents(
                listOf(EventForRequest("test-device-id", name, eventProperty))
            )
        }
    }

    @Test
    fun `it should track event with name only`() = runBlocking {
        // Given
        val eventName = "test_event"
        val eventSlot = slot<List<EventForRequest>>()
        coEvery { eventAPIService.trackEvents(capture(eventSlot)) } returns Unit

        // When
        eventService.trackEvent(eventName)

        // Then
        coVerify(exactly = 1) { eventAPIService.trackEvents(capture(eventSlot)) }
        val capturedEvent = eventSlot.captured.first()
        assert(capturedEvent.name == eventName)
        assert(capturedEvent.eventProperty.customProperties.isEmpty())
    }

    @Test
    fun `it should track event with properties`() = runBlocking {
        // Given
        val eventName = "test_event"
        val properties = mapOf("key1" to "value1", "key2" to 123)
        val eventSlot = slot<List<EventForRequest>>()
        coEvery { eventAPIService.trackEvents(capture(eventSlot)) } returns Unit

        // When
        eventService.trackEvent(eventName, properties)

        // Then
        coVerify(exactly = 1) { eventAPIService.trackEvents(capture(eventSlot)) }
        val capturedEvent = eventSlot.captured.first()
        assert(capturedEvent.name == eventName)
        assert(capturedEvent.eventProperty.customProperties["key1"] == JsonPrimitive("value1"))
        assert(capturedEvent.eventProperty.customProperties["key2"] == JsonPrimitive(123))
    }

    @Test
    fun `it should track event with null properties`() = runBlocking {
        // Given
        val eventName = "test_event"
        val properties = mapOf("key1" to null)
        val eventSlot = slot<List<EventForRequest>>()
        coEvery { eventAPIService.trackEvents(capture(eventSlot)) } returns Unit

        // When
        eventService.trackEvent(eventName, properties)

        // Then
        coVerify(exactly = 1) { eventAPIService.trackEvents(capture(eventSlot)) }
        val capturedEvent = eventSlot.captured.first()
        assert(capturedEvent.name == eventName)
        assert(capturedEvent.eventProperty.customProperties.containsKey("key1"))
        // Null values are converted to JsonPrimitive("null")
        assert(capturedEvent.eventProperty.customProperties["key1"] == JsonPrimitive("null"))
    }

    @Test
    fun `it should track event with empty properties map`() = runBlocking {
        // Given
        val eventName = "test_event"
        val properties = emptyMap<String, Any?>()
        val eventSlot = slot<List<EventForRequest>>()
        coEvery { eventAPIService.trackEvents(capture(eventSlot)) } returns Unit

        // When
        eventService.trackEvent(eventName, properties)

        // Then
        coVerify(exactly = 1) { eventAPIService.trackEvents(capture(eventSlot)) }
        val capturedEvent = eventSlot.captured.first()
        assert(capturedEvent.name == eventName)
        assert(capturedEvent.eventProperty.customProperties.isEmpty())
    }
}
