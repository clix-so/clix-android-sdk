package so.clix.utils.http

import java.net.URL

internal data class HTTPRequest<Req>(
    val url: URL,
    val method: HTTPMethod = HTTPMethod.GET,
    val params: Map<String, Any>? = null,
    val headers: Map<String, String>? = null,
    val data: Req? = null,
)
