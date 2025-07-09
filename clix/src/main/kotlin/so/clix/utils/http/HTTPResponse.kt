package so.clix.utils.http

internal data class HTTPResponse<Res>(
    val data: Res,
    val statusCode: Int,
    val headers: Map<String, List<String>>,
)
