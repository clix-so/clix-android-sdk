package so.clix.services

import ClixError
import java.io.IOException
import java.util.UUID
import kotlinx.serialization.SerializationException
import so.clix.core.Clix
import so.clix.models.ClixUserProperty
import so.clix.utils.logging.ClixLogger

internal class DeviceService(private val storageService: StorageService) {
    private val deviceApiService = DeviceAPIService()
    private val deviceIdKey = "clix_device_id"

    fun getCurrentDeviceId(): String {
        val deviceId = storageService.get<String>(deviceIdKey)
        if (deviceId != null) {
            return deviceId
        }
        val newDeviceId = UUID.randomUUID().toString()
        storageService.set(deviceIdKey, newDeviceId)
        return newDeviceId
    }

    suspend fun setProjectUserId(projectUserId: String) {
        deviceApiService.setProjectUserId(Clix.environment.deviceId, projectUserId)
    }

    suspend fun updateUserProperties(properties: Map<String, Any>) {
        try {
            val propertiesList =
                properties.map { (name, value) -> ClixUserProperty.of(name, value) }
            deviceApiService.upsertUserProperties(Clix.environment.deviceId, propertiesList)
            ClixLogger.debug(message = "Updated user properties: $properties")
        } catch (e: IOException) {
            ClixLogger.error("Network error during updating user properties", e)
        } catch (e: SerializationException) {
            ClixLogger.error("JSON parsing error during updating user properties", e)
        } catch (e: ClixError.InvalidResponse) {
            ClixLogger.error("Failed to set user properties: ${e.message}", e)
        }
    }

    suspend fun upsertToken(token: String) {
        try {
            val device = Clix.environment.getDevice().copy(pushToken = token)
            Clix.environment.setDevice(device)
            deviceApiService.upsertDevice(device)
            ClixLogger.debug("Registered device token")
        } catch (e: IOException) {
            ClixLogger.error("Network error during registering device", e)
        } catch (e: SerializationException) {
            ClixLogger.error("JSON parsing error during registering device", e)
        } catch (e: ClixError.InvalidResponse) {
            ClixLogger.error("Failed to register device: ${e.message}", e)
        }
    }
}
