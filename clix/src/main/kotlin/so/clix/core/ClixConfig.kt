package so.clix.core

import kotlinx.serialization.Serializable
import so.clix.utils.logging.ClixLogLevel

/**
 * Configuration for the Clix SDK.
 *
 * @property projectId The project identifier for Clix services.
 * @property apiKey The API key for accessing Clix services.
 * @property endpoint The API endpoint URL.
 * @property logLevel The log level for the SDK.
 * @property extraHeaders Extra headers to be included in API requests.
 */
@Serializable
data class ClixConfig(
    val projectId: String,
    val apiKey: String,
    val endpoint: String = "https://api.clix.so",
    val logLevel: ClixLogLevel = ClixLogLevel.INFO,
    val extraHeaders: Map<String, String> = emptyMap(),
)
