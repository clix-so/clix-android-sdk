package so.clix.models

import kotlinx.serialization.Serializable
import so.clix.utils.ClixDateFormatter

@Serializable
internal data class ClixUserProperty(
    val name: String,
    val type: UserPropertyType,
    val value: String? = null,
) {
    enum class UserPropertyType {
        USER_PROPERTY_TYPE_STRING,
        USER_PROPERTY_TYPE_NUMBER,
        USER_PROPERTY_TYPE_BOOLEAN,
        USER_PROPERTY_TYPE_DATETIME,
    }

    companion object {
        fun of(name: String, value: Any): ClixUserProperty {
            return when (value) {
                is Boolean ->
                    ClixUserProperty(
                        name,
                        UserPropertyType.USER_PROPERTY_TYPE_BOOLEAN,
                        value.toString(),
                    )
                is Number ->
                    ClixUserProperty(
                        name,
                        UserPropertyType.USER_PROPERTY_TYPE_NUMBER,
                        value.toString(),
                    )
                is String ->
                    ClixUserProperty(name, UserPropertyType.USER_PROPERTY_TYPE_STRING, value)
                else ->
                    ClixDateFormatter.format(value)?.let { isoString ->
                        ClixUserProperty(
                            name,
                            UserPropertyType.USER_PROPERTY_TYPE_DATETIME,
                            isoString,
                        )
                    }
                        ?: ClixUserProperty(
                            name,
                            UserPropertyType.USER_PROPERTY_TYPE_STRING,
                            value.toString(),
                        )
            }
        }
    }
}
