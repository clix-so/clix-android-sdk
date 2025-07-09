package so.clix.models

import kotlinx.serialization.Serializable

@Serializable
internal data class ClixDevice(
    val id: String,
    val platform: String,
    val model: String,
    val manufacturer: String,
    val osName: String,
    val osVersion: String,
    val localeRegion: String,
    val localeLanguage: String,
    val timezone: String,
    val appName: String,
    val appVersion: String?,
    val sdkType: String,
    val sdkVersion: String,
    val adId: String?,
    val isPushPermissionGranted: Boolean,
    val pushToken: String,
    val pushTokenType: String,
)
