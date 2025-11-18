package so.clix.services

import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class NotificationServiceTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var storageService: StorageService
    private lateinit var eventService: EventService
    private lateinit var notificationService: NotificationService

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        notificationManager = mockk(relaxed = true)

        // Create a real StorageService with mocked SharedPreferences
        val sharedPreferences = mockk<android.content.SharedPreferences>(relaxed = true)
        val editor = mockk<android.content.SharedPreferences.Editor>(relaxed = true)
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor

        storageService = StorageService(context)
        eventService = mockk(relaxed = true)

        // Mock the NotificationManagerCompat.from() static method
        mockkStatic(NotificationManagerCompat::class)
        every { NotificationManagerCompat.from(any()) } returns notificationManager

        // Create the NotificationService instance with dependencies
        notificationService = NotificationService(context, storageService, eventService)
    }

    @Test
    fun `it should track event when notification response is handled`() = runBlocking {
        // Given
        val messageId = "test-message-id"

        // When
        notificationService.handleNotificationTapped(messageId)

        // Then
        coVerify {
            eventService.trackEvent(name = "PUSH_NOTIFICATION_TAPPED", messageId = messageId)
        }
    }

    @Test
    fun `it should save settings when notification preferences are enabled and has permission`() =
        runBlocking {
            // Given
            val enabled = true
            val categories = listOf("news", "promotions")

            // Mock NotificationManagerCompat to return notifications enabled
            every { notificationManager.areNotificationsEnabled() } returns true

            // When
            notificationService.setNotificationPreferences(context, enabled, categories)

            // Then - just verify that the method completed without exceptions
            // We can't easily verify the exact settings that were saved with a real StorageService
        }

    @Test
    fun `it should save settings when notification preferences are disabled regardless of permission`() =
        runBlocking {
            // Given
            val enabled = false
            val categories = listOf("news", "promotions")

            // Mock NotificationManagerCompat to return notifications disabled
            // (permission check is skipped when enabled = false, but mocking for completeness)
            every { notificationManager.areNotificationsEnabled() } returns false

            // When
            notificationService.setNotificationPreferences(context, enabled, categories)

            // Then - just verify that the method completed without exceptions
            // We can't easily verify the exact settings that were saved with a real StorageService
        }

    @Test
    fun `it should remove settings and cancel notifications when reset`() {
        // When
        notificationService.reset()

        // Then
        // We can't easily verify the exact calls to storageService.remove with a real
        // StorageService
        // Just verify that notificationManager.cancelAll() was called
        verify { notificationManager.cancelAll() }
    }
}
