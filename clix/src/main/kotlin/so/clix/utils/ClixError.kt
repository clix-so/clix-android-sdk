internal sealed class ClixError(override val message: String?) : Exception(message) {

    object NotInitialized : ClixError("Clix SDK is not initialized. Call Clix.initialize() first.")

    object NotificationPermissionDenied : ClixError("Notification permission denied.")

    data class InvalidConfiguration(val reason: String) :
        ClixError("Invalid SDK configuration: $reason")

    object InvalidURL : ClixError("The provided URL is invalid.")

    object InvalidResponse : ClixError("The response was invalid or permission was denied.")

    data class NetworkError(val underlyingError: Throwable?) :
        ClixError(
            underlyingError?.let { "Network request failed: ${it.localizedMessage}" }
                ?: "An unspecified network error occurred."
        )

    object EncodingError : ClixError("Failed to encode request body.")

    data class DecodingError(val underlyingError: Throwable) :
        ClixError("Failed to decode response body: ${underlyingError.localizedMessage}")

    data class UnknownError(val underlyingError: Throwable?) :
        ClixError(
            underlyingError?.let { "An unknown error occurred: ${it.localizedMessage}" }
                ?: "An unknown error occurred."
        )

    override fun toString(): String = message ?: "Unknown error"
}
