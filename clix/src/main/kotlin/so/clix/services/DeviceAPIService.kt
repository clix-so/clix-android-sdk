package so.clix.services

import kotlinx.serialization.Serializable
import so.clix.models.ClixDevice
import so.clix.models.ClixServerDevice
import so.clix.models.ClixUserProperty

@Serializable internal data class CreateOrUpdateDevicesRequest(val devices: List<ClixDevice>)

@Serializable
internal data class CreateOrUpdateDevicesResponse(val devices: List<ClixServerDevice>)

@Serializable internal data class SetProjectUserIdForDeviceRequest(val projectUserId: String)

@Serializable
internal data class SetProjectUserIdForDeviceResponse(
    val deviceId: String,
    val projectUserId: String,
)

@Serializable
internal data class CreateOrUpdateUsersPropertiesByDeviceIdRequest(
    val properties: List<ClixUserProperty>
)

@Serializable
internal data class CreateOrUpdateUsersPropertiesByDeviceIdResponse(
    val properties: List<ClixUserProperty>
)

internal class DeviceAPIService : ClixAPIClient() {
    suspend fun upsertDevice(device: ClixDevice): ClixServerDevice {
        val data = CreateOrUpdateDevicesRequest(devices = listOf(device))
        val response =
            post<CreateOrUpdateDevicesRequest, CreateOrUpdateDevicesResponse>(
                path = "/devices",
                data = data,
            )
        return response.devices[0]
    }

    suspend fun setProjectUserId(deviceId: String, projectUserId: String): String {
        val data = SetProjectUserIdForDeviceRequest(projectUserId = projectUserId)
        val response =
            post<SetProjectUserIdForDeviceRequest, SetProjectUserIdForDeviceResponse>(
                path = "/devices/${deviceId}/user/project-user-id",
                data = data,
            )
        return response.projectUserId
    }

    suspend fun upsertUserProperties(deviceId: String, properties: List<ClixUserProperty>) {
        val data = CreateOrUpdateUsersPropertiesByDeviceIdRequest(properties = properties)
        post<
            CreateOrUpdateUsersPropertiesByDeviceIdRequest,
            CreateOrUpdateUsersPropertiesByDeviceIdResponse,
        >(
            path = "/devices/${deviceId}/user/properties",
            data = data,
        )
    }
}
