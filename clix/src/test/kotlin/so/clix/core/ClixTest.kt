package so.clix.core

import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import so.clix.services.DeviceService
import so.clix.services.EventService
import so.clix.services.NotificationService
import so.clix.services.TokenService
import so.clix.utils.logging.ClixLogLevel
import so.clix.utils.logging.ClixLogger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class ClixTest {

    private lateinit var context: Context
    private lateinit var config: ClixConfig
    private lateinit var deviceService: DeviceService
    private lateinit var eventService: EventService
    private lateinit var tokenService: TokenService
    private lateinit var notificationService: NotificationService

    private lateinit var notificationManager: NotificationManagerCompat

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        config = ClixConfig(projectId = "test-project-id", apiKey = "test-api-name")
        deviceService = mockk(relaxed = true)
        eventService = mockk(relaxed = true)
        tokenService = mockk(relaxed = true)
        notificationService = mockk(relaxed = true)
        notificationManager = mockk(relaxed = true)

        // Mock application context
        every { context.applicationContext } returns context

        // Mock the NotificationManagerCompat.from() static method
        mockkStatic(NotificationManagerCompat::class)
        every { NotificationManagerCompat.from(any()) } returns notificationManager

        // Mock ClixLogger
        mockkObject(ClixLogger)
        every { ClixLogger.setLogLevel(any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun initializeAndInjectMocks() {
        // Mock the Clix object
        mockkObject(Clix)

        // Mock the services
        every { Clix.deviceService } returns deviceService
        every { Clix.eventService } returns eventService
        every { Clix.tokenService } returns tokenService
        every { Clix.notificationService } returns notificationService

        // Create a mock environment
        val mockEnvironment = mockk<ClixEnvironment>()
        every { mockEnvironment.config } returns config
        every { mockEnvironment.deviceId } returns "test-device-id"
        every { Clix.environment } returns mockEnvironment
    }

    @Test
    fun `it should initialize SDK with provided config`() {
        // Given
        val customConfig =
            ClixConfig(
                projectId = "test-project-id",
                apiKey = "custom-api-name",
                endpoint = "https://custom-endpoint.com",
                logLevel = ClixLogLevel.DEBUG,
            )

        // When
        Clix.initialize(context, customConfig)

        // Then
        verify { ClixLogger.setLogLevel(ClixLogLevel.DEBUG) }

        // Since we're testing the actual initialization, we need to verify that
        // the services were created with the right dependencies
        verify { context.applicationContext }
    }

    @Test
    fun `it should set user ID`() = runBlocking {
        // Given
        initializeAndInjectMocks()
        val userId = "test-user-id"

        // When
        Clix.setUserId(userId)

        // Then
        coVerify { deviceService.setProjectUserId(userId) }
    }

    @Test
    fun `it should set user property`() = runBlocking {
        // Given
        initializeAndInjectMocks()
        val name = "test-name"
        val value = "test-value"

        // When
        Clix.setUserProperty(name, value)

        // Then
        coVerify { deviceService.updateUserProperties(mapOf(name to value)) }
    }

    @Test
    fun `it should set user properties`() = runBlocking {
        // Given
        initializeAndInjectMocks()
        val properties = mapOf("name1" to "value1", "name2" to 123)

        // When
        Clix.setUserProperties(properties)

        // Then
        coVerify { deviceService.updateUserProperties(properties) }
    }

    @Test
    fun `it should track event with name only`() = runBlocking {
        // Given
        initializeAndInjectMocks()
        val eventName = "test-event"

        // When
        Clix.trackEvent(eventName)

        // Then
        coVerify { eventService.trackEvent(eventName, emptyMap()) }
    }

    @Test
    fun `it should track event with properties`() = runBlocking {
        // Given
        initializeAndInjectMocks()
        val eventName = "test-event"
        val properties = mapOf("name1" to "value1", "name2" to 123)

        // When
        Clix.trackEvent(eventName, properties)

        // Then
        coVerify { eventService.trackEvent(eventName, properties) }
    }

    @Test
    fun `it should set log level`() {
        // Given
        initializeAndInjectMocks()
        val logLevel = ClixLogLevel.DEBUG

        // When
        Clix.setLogLevel(logLevel)

        // Then
        verify { ClixLogger.setLogLevel(logLevel) }
    }

    @Test
    fun `it should get device ID`() {
        // Given
        initializeAndInjectMocks()
        val expectedDeviceId = "test-device-id"
        every { deviceService.getCurrentDeviceId() } returns expectedDeviceId

        // When
        val actualDeviceId = Clix.getDeviceId()

        // Then
        assertEquals(expectedDeviceId, actualDeviceId)
        verify { deviceService.getCurrentDeviceId() }
    }

    @Test
    fun `it should handle exception when getting device ID`() {
        // Given
        initializeAndInjectMocks()
        every { deviceService.getCurrentDeviceId() } throws RuntimeException("Test exception")

        // When
        val result = Clix.getDeviceId()

        // Then
        assertEquals("", result)
        verify { deviceService.getCurrentDeviceId() }
        verify { ClixLogger.error("Failed to get device ID", any()) }
    }

    @Test
    fun `it should get token`() {
        // Given
        initializeAndInjectMocks()
        val expectedToken = "test-token"
        every { tokenService.getCurrentToken() } returns expectedToken

        // When
        val actualToken = Clix.getToken()

        // Then
        assertEquals(expectedToken, actualToken)
        verify { tokenService.getCurrentToken() }
    }

    @Test
    fun `it should handle exception when getting token`() {
        // Given
        initializeAndInjectMocks()
        every { tokenService.getCurrentToken() } throws RuntimeException("Test exception")

        // When
        val result = Clix.getToken()

        // Then
        assertEquals(null, result)
        verify { tokenService.getCurrentToken() }
        verify { ClixLogger.error("Failed to get token", any()) }
    }
}
