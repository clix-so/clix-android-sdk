package so.clix.services

import android.content.Context
import android.os.Build
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class StorageServiceTest {

    private lateinit var context: Context
    private lateinit var storageService: StorageService

    @Serializable data class TestData(val name: String, val value: Int)

    @Before
    fun setup() {
        // Use Robolectric's application context
        context = RuntimeEnvironment.getApplication()

        // Create a real StorageService with the Robolectric context
        storageService = StorageService(context)

        // Clear any existing preferences before each test
        val sharedPreferences =
            context.getSharedPreferences("clix_preferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
    }

    @Test
    fun `it should save string value to shared preferences`() {
        // Given
        val key = "test_key"
        val value = "test_value"

        // When
        storageService.set(key, value)

        // Then
        val result: String? = storageService.get(key)
        assertEquals(value, result)
    }

    @Test
    fun `it should save complex object to shared preferences`() {
        // Given
        val key = "test_key"
        val value = TestData("test", 123)

        // When
        storageService.set(key, value)

        // Then
        val result: TestData? = storageService.get(key)
        assertEquals(value, result)
    }

    @Test
    fun `it should remove key when null value is set`() {
        // Given
        val key = "test_key"
        val initialValue = "test_value"
        storageService.set(key, initialValue)

        // Verify the value was set
        val initialResult: String? = storageService.get(key)
        assertEquals(initialValue, initialResult)

        // When
        storageService.set(key, null as String?)

        // Then
        val result: String? = storageService.get(key)
        assertNull(result)
    }

    @Test
    fun `it should retrieve string value from shared preferences`() {
        // Given
        val key = "test_key"
        val value = "test_value"
        storageService.set(key, value)

        // When
        val result: String? = storageService.get(key)

        // Then
        assertEquals(value, result)
    }

    @Test
    fun `it should retrieve complex object from shared preferences`() {
        // Given
        val key = "test_key"
        val value = TestData("test", 123)
        storageService.set(key, value)

        // When
        val result: TestData? = storageService.get(key)

        // Then
        assertEquals(value, result)
    }

    @Test
    fun `it should return null when key is not found`() {
        // Given
        val key = "nonexistent_key"

        // When
        val result: String? = storageService.get(key)

        // Then
        assertNull(result)
    }

    @Test
    fun `it should return null when deserialization fails`() {
        // Given
        val key = "test_key"

        // Manually insert invalid JSON using SharedPreferences directly
        val sharedPreferences =
            context.getSharedPreferences("clix_preferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(key, "{invalid json}").apply()

        // When
        val result: TestData? = storageService.get(key)

        // Then
        assertNull(result)
    }

    @Test
    fun `it should remove value from shared preferences`() {
        // Given
        val key = "test_key"
        val value = "test_value"
        storageService.set(key, value)

        // Verify the value was set
        val initialResult: String? = storageService.get(key)
        assertEquals(value, initialResult)

        // When
        storageService.remove(key)

        // Then
        val result: String? = storageService.get(key)
        assertNull(result)
    }
}
