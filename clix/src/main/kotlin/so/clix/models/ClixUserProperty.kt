package so.clix.models

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlinx.serialization.Serializable

@Serializable
internal data class ClixUserProperty(
    val name: String,
    val type: UserPropertyType,
    val valueString: String,
) {
    enum class UserPropertyType {
        USER_PROPERTY_TYPE_STRING,
        USER_PROPERTY_TYPE_NUMBER,
        USER_PROPERTY_TYPE_BOOLEAN,
        USER_PROPERTY_TYPE_DATETIME,
    }

    companion object {
        private fun convertToInstant(value: Any): Instant? {
            return when (value) {
                is Date -> value.toInstant()
                is Calendar -> value.toInstant()
                is ZonedDateTime -> value.toInstant()
                is LocalDateTime -> value.atZone(ZoneId.systemDefault()).toInstant()
                is LocalDate -> value.atStartOfDay(ZoneId.systemDefault()).toInstant()
                is Instant -> value
                else -> null
            }
        }

        private fun formatInstantToIso8601(instant: Instant): String {
            return instant
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        }

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
                    convertToInstant(value)?.let { instant ->
                        ClixUserProperty(
                            name,
                            UserPropertyType.USER_PROPERTY_TYPE_DATETIME,
                            formatInstantToIso8601(instant),
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
