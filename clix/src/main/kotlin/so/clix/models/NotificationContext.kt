package so.clix.models

internal data class NotificationContext(
    val notificationData: Map<String, Any?> = emptyMap(),
    val autoOpenLandingURL: Boolean = true,
)
