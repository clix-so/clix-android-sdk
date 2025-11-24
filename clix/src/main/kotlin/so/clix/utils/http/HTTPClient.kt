package so.clix.utils.http

import android.net.TrafficStats
import android.util.LruCache
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import so.clix.utils.logging.ClixLogger

internal class HTTPClient(private val json: Json) {
    private val delayUntilMap = mutableMapOf<String, Long>()
    private val eTagCache = LruCache<String, String>(100)
    private val responseCache = LruCache<String, String>(100)

    private fun buildRequestURL(url: String, params: Map<String, Any>?): String =
        if (params.isNullOrEmpty()) {
            url
        } else {
            val queryParams =
                params.entries.flatMap { (k, v) ->
                    when (v) {
                        is Collection<*> ->
                            v.map { item ->
                                val encodedKey = URLEncoder.encode(k, CHARSET_UTF8)
                                val encodedValue = URLEncoder.encode(item.toString(), CHARSET_UTF8)
                                "$encodedKey=$encodedValue"
                            }
                        else -> {
                            val encodedKey = URLEncoder.encode(k, CHARSET_UTF8)
                            val encodedValue = URLEncoder.encode(v.toString(), CHARSET_UTF8)
                            listOf("$encodedKey=$encodedValue")
                        }
                    }
                }
            queryParams.joinToString(QUERY_PARAM_SEPARATOR, "$url$QUERY_PREFIX")
        }

    private fun setupConnection(url: String, httpRequest: HTTPRequest<*>): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = httpRequest.method.name
            connectTimeout = DEFAULT_TIMEOUT
            readTimeout = DEFAULT_TIMEOUT
            setRequestProperty(HEADER_CONTENT_TYPE, MEDIA_TYPE_JSON)
            setRequestProperty(HEADER_ACCEPT, MEDIA_TYPE_JSON)
            httpRequest.headers?.forEach { (k, v) -> setRequestProperty(k, v) }
            httpRequest.headers?.get(HEADER_CACHE_KEY)?.let { key ->
                eTagCache.get(key)?.let { setRequestProperty(HEADER_IF_NONE_MATCH, it) }
            }
        }

    private fun writeRequestBody(conn: HttpURLConnection, body: String) {
        conn.doOutput = true
        conn.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
    }

    private fun readResponse(conn: HttpURLConnection): String {
        val status = conn.responseCode
        if (status == HttpURLConnection.HTTP_NOT_MODIFIED) {
            val cacheKey = conn.getRequestProperty(HEADER_CACHE_KEY)
            return cacheKey?.let { responseCache.get(it) } ?: EMPTY_BODY
        }
        val reader =
            if (status in STATUS_SUCCESSFUL_MIN..STATUS_SUCCESSFUL_MAX) {
                BufferedReader(InputStreamReader(conn.inputStream))
            } else {
                BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream))
            }
        return reader.use { it.readText() }
    }

    private fun handleCaching(
        conn: HttpURLConnection,
        text: String,
        headers: Map<String, String>?,
    ) {
        headers?.get(HEADER_CACHE_KEY)?.let { key ->
            conn.getHeaderField(HEADER_ETAG)?.let { etag ->
                eTagCache.put(key, etag)
                responseCache.put(key, text)
            }
        }
    }

    private fun calculateRetryAfter(conn: HttpURLConnection, status: Int): Long? =
        conn.getHeaderField(HEADER_RETRY_AFTER)?.toLongOrNull()
            ?: if (status == STATUS_TOO_MANY_REQUESTS) DEFAULT_RETRY_FALLBACK.toLong() else null

    suspend inline fun <reified Req : Any, reified Res : Any> request(
        httpRequest: HTTPRequest<Req>
    ): HTTPResponse<Res> =
        withContext(Dispatchers.IO) {
            val delayKey =
                "${httpRequest.method.name}:${httpRequest.url.host}${httpRequest.url.path}"
            val now = System.currentTimeMillis()
            val delayMillis = (delayUntilMap[delayKey] ?: 0L) - now
            if (delayMillis > 0) delay(delayMillis)
            try {
                return@withContext withTimeout((DEFAULT_TIMEOUT + TIMEOUT_BUFFER).toLong()) {
                    TrafficStats.setThreadStatsTag(THREAD_ID)
                    val url = buildRequestURL(httpRequest.url.toString(), httpRequest.params)
                    val conn = setupConnection(url, httpRequest)
                    if (
                        httpRequest.data != null && METHODS_WITH_BODY.contains(httpRequest.method)
                    ) {
                        val body = json.encodeToString<Req>(httpRequest.data)
                        ClixLogger.debug(
                            "HTTP Request: ${httpRequest.method} $url, body=$body, headers=${httpRequest.headers}"
                        )
                        writeRequestBody(conn, body)
                    } else {
                        ClixLogger.debug(
                            "HTTP Request: ${httpRequest.method} $url, headers=${httpRequest.headers}"
                        )
                    }
                    val status = conn.responseCode
                    val text = readResponse(conn)
                    ClixLogger.debug("HTTP Response: status=$status, body=$text")
                    handleCaching(conn, text, httpRequest.headers)
                    calculateRetryAfter(conn, status)?.let {
                        delayUntilMap[delayKey] = System.currentTimeMillis() + it * 1000
                    }
                    val parsed = json.decodeFromString<Res>(text)
                    HTTPResponse(parsed, status, conn.headerFields.filterKeys { true })
                }
            } catch (e: TimeoutCancellationException) {
                ClixLogger.error(
                    "HTTPClient: Request timed out for ${httpRequest.url}: ${e.message}",
                    e,
                )
                throw e
            } catch (e: ConnectException) {
                ClixLogger.error("HTTPClient: Network offline: ${e.message}", e)
                throw e
            } catch (e: UnknownHostException) {
                ClixLogger.error("HTTPClient: Unknown host: ${e.message}", e)
                throw e
            } catch (e: Throwable) {
                ClixLogger.error("HTTPClient: Error during request: ${e.message}", e)
                throw e
            }
        }

    suspend inline fun <reified Res : Any> get(
        url: URL,
        params: Map<String, Any>? = null,
        headers: Map<String, String>? = null,
    ): HTTPResponse<Res> = request(HTTPRequest(url, HTTPMethod.GET, params, headers))

    suspend inline fun <reified Req : Any, reified Res : Any> post(
        url: URL,
        data: Req,
        params: Map<String, Any>? = null,
        headers: Map<String, String>? = null,
    ): HTTPResponse<Res> = request(HTTPRequest(url, HTTPMethod.POST, params, headers, data))

    suspend inline fun <reified Req : Any, reified Res : Any> put(
        url: URL,
        data: Req,
        params: Map<String, Any>? = null,
        headers: Map<String, String>? = null,
    ): HTTPResponse<Res> = request(HTTPRequest(url, HTTPMethod.PUT, params, headers, data))

    suspend inline fun <reified Res : Any> delete(
        url: URL,
        params: Map<String, Any>? = null,
        headers: Map<String, String>? = null,
    ): HTTPResponse<Res> = request(HTTPRequest(url, HTTPMethod.DELETE, params, headers))

    companion object {
        val INSTANCE = createInstance()

        private val METHODS_WITH_BODY = listOf(HTTPMethod.POST, HTTPMethod.PUT)
        private const val THREAD_ID = 10001
        private const val DEFAULT_TIMEOUT = 15000
        private const val TIMEOUT_BUFFER = 5000
        private const val DEFAULT_RETRY_FALLBACK = 60

        private const val CHARSET_UTF8 = "UTF-8"
        private const val HEADER_CONTENT_TYPE = "Content-Type"
        private const val HEADER_ACCEPT = "Accept"
        private const val MEDIA_TYPE_JSON = "application/json"
        private const val HEADER_IF_NONE_MATCH = "If-None-Match"
        private const val HEADER_CACHE_KEY = "Cache-Key"
        private const val HEADER_RETRY_AFTER = "Retry-After"
        private const val HEADER_ETAG = "ETag"
        private const val STATUS_SUCCESSFUL_MIN = 200
        private const val STATUS_SUCCESSFUL_MAX = 299
        private const val STATUS_TOO_MANY_REQUESTS = 429
        private const val QUERY_PREFIX = "?"
        private const val QUERY_PARAM_SEPARATOR = "&"
        private const val EMPTY_BODY = ""

        fun createInstance(json: Json = Json { ignoreUnknownKeys = true }): HTTPClient {
            return HTTPClient(json)
        }
    }
}
