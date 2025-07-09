package so.clix.services

import ClixError
import java.net.MalformedURLException
import java.net.URL
import kotlinx.serialization.json.Json
import so.clix.core.Clix
import so.clix.utils.http.HTTPClient
import so.clix.utils.http.HTTPResponse
import so.clix.utils.logging.ClixLogger

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
internal open class ClixAPIClient {
    internal val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient: HTTPClient = HTTPClient.createInstance(json)

    private val apiBasePath = "/api/v1"

    private fun getDefaultHeaders(): Map<String, String> =
        mapOf(
            "X-Clix-Project-ID" to Clix.environment.config.projectId,
            "X-Clix-API-Key" to Clix.environment.config.apiKey,
            "X-Clix-App-Identifier" to Clix.environment.getDevice().appName,
            "User-Agent" to "clix-android-sdk@${Clix.VERSION}",
        ) + Clix.environment.config.extraHeaders

    private fun buildURL(path: String): URL {
        val baseUrl =
            try {
                URL(Clix.environment.config.endpoint)
            } catch (e: MalformedURLException) {
                throw ClixError.InvalidURL
            }
        return baseUrl.toURI().resolve(apiBasePath + path).toURL()
    }

    suspend inline fun <reified Res : Any> get(
        path: String,
        params: Map<String, Any>? = null,
    ): Res {
        val url = buildURL(path)
        val headers = getDefaultHeaders()
        ClixLogger.debug("ClixAPIClient GET Request: URL: $url, Params: $params, Headers: $headers")
        try {
            val response: HTTPResponse<Res> = httpClient.get<Res>(url, params, headers)
            ClixLogger.debug(
                "ClixAPIClient GET Response: URL: $url, Status: ${response.statusCode}, Data: ${response.data}"
            )
            return response.data
        } catch (e: Exception) {
            ClixLogger.error(
                "ClixAPIClient GET Request Failed: URL: $url, Params: $params, Headers: $headers",
                e,
            )
            throw e
        }
    }

    suspend inline fun <reified Req : Any, reified Res : Any> post(
        path: String,
        data: Req,
        params: Map<String, Any>? = null,
    ): Res {
        val url = buildURL(path)
        val headers = getDefaultHeaders()
        ClixLogger.debug(
            "ClixAPIClient POST Request: URL: $url, Data: $data, Params: $params, Headers: $headers"
        )
        try {
            val response: HTTPResponse<Res> = httpClient.post<Req, Res>(url, data, params, headers)
            ClixLogger.debug(
                "ClixAPIClient POST Response: URL: $url, Status: ${response.statusCode}, Data: ${response.data}"
            )
            return response.data
        } catch (e: Exception) {
            ClixLogger.error(
                "ClixAPIClient POST Request Failed: URL: $url, Data: $data, Params: $params, Headers: $headers",
                e,
            )
            throw e
        }
    }

    suspend inline fun <reified Req : Any, reified Res : Any> put(
        path: String,
        data: Req,
        params: Map<String, Any>? = null,
    ): Res {
        val url = buildURL(path)
        val headers = getDefaultHeaders()
        ClixLogger.debug(
            "ClixAPIClient PUT Request: URL: $url, Data: $data, Params: $params, Headers: $headers"
        )
        try {
            val response: HTTPResponse<Res> = httpClient.put<Req, Res>(url, data, params, headers)
            ClixLogger.debug(
                "ClixAPIClient PUT Response: URL: $url, Status: ${response.statusCode}, Data: ${response.data}"
            )
            return response.data
        } catch (e: Exception) {
            ClixLogger.error(
                "ClixAPIClient PUT Request Failed: URL: $url, Data: $data, Params: $params, Headers: $headers",
                e,
            )
            throw e
        }
    }

    suspend inline fun <reified Res : Any> delete(
        path: String,
        params: Map<String, Any>? = null,
    ): Res {
        val url = buildURL(path)
        val headers = getDefaultHeaders()
        ClixLogger.debug(
            "ClixAPIClient DELETE Request: URL: $url, Params: $params, Headers: $headers"
        )
        try {
            val response: HTTPResponse<Res> = httpClient.delete<Res>(url, params, headers)
            ClixLogger.debug(
                "ClixAPIClient DELETE Response: URL: $url, Status: ${response.statusCode}, Data: ${response.data}"
            )
            return response.data
        } catch (e: Exception) {
            ClixLogger.error(
                "ClixAPIClient DELETE Request Failed: URL: $url, Params: $params, Headers: $headers",
                e,
            )
            throw e
        }
    }
}
