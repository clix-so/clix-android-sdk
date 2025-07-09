package so.clix.core

import org.junit.Assert.assertEquals
import org.junit.Test
import so.clix.utils.logging.ClixLogLevel

class ClixConfigTest {

    @Test
    fun `it should create config with default values when required fields are provided`() {
        // Given
        val projectId = "test-project-id"
        val apiKey = "test-api-key"

        // When
        val config = ClixConfig(projectId = projectId, apiKey = apiKey)

        // Then
        assertEquals(projectId, config.projectId)
        assertEquals(apiKey, config.apiKey)
        assertEquals("https://api.clix.so", config.endpoint)
        assertEquals(ClixLogLevel.INFO, config.logLevel)
    }

    @Test
    fun `it should create config with custom endpoint when provided`() {
        // Given
        val projectId = "test-project-id"
        val apiKey = "test-api-key"
        val customEndpoint = "https://custom-api.clix.so"

        // When
        val config = ClixConfig(projectId = projectId, apiKey = apiKey, endpoint = customEndpoint)

        // Then
        assertEquals(projectId, config.projectId)
        assertEquals(apiKey, config.apiKey)
        assertEquals(customEndpoint, config.endpoint)
        assertEquals(ClixLogLevel.INFO, config.logLevel)
    }

    @Test
    fun `it should create config with custom log level when provided`() {
        // Given
        val projectId = "test-project-id"
        val apiKey = "test-api-key"
        val customLogLevel = ClixLogLevel.DEBUG

        // When
        val config = ClixConfig(projectId = projectId, apiKey = apiKey, logLevel = customLogLevel)

        // Then
        assertEquals(apiKey, config.apiKey)
        assertEquals("https://api.clix.so", config.endpoint)
        assertEquals(customLogLevel, config.logLevel)
    }

    @Test
    fun `it should create config with default log level when provided`() {
        // Given
        val projectId = "test-project-id"
        val apiKey = "test-api-key"

        // When
        val config = ClixConfig(projectId = projectId, apiKey = apiKey)

        // Then
        assertEquals(apiKey, config.apiKey)
        assertEquals("https://api.clix.so", config.endpoint)
        assertEquals(ClixLogLevel.INFO, config.logLevel)
    }

    @Test
    fun `it should create config with custom headers when provided`() {
        // Given
        val projectId = "test-project-id"
        val apiKey = "test-api-key"
        val customHeaders = mapOf("X-Custom-Header" to "custom-value")

        // When
        val config =
            ClixConfig(projectId = projectId, apiKey = apiKey, extraHeaders = customHeaders)

        // Then
        assertEquals(projectId, config.projectId)
        assertEquals(apiKey, config.apiKey)
        assertEquals("https://api.clix.so", config.endpoint)
        assertEquals(ClixLogLevel.INFO, config.logLevel)
        assertEquals(customHeaders, config.extraHeaders)
    }
}
