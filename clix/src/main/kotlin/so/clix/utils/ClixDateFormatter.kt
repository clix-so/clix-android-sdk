package so.clix.utils

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date

internal object ClixDateFormatter {
    private val timeZone: ZoneId = ZoneId.systemDefault()
    private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun format(value: Any?): String? {
        val instant = convertToInstant(value) ?: return null
        return instant.atZone(timeZone).format(formatter)
    }

    private fun convertToInstant(value: Any?): Instant? {
        return when (value) {
            is Date -> value.toInstant()
            is Calendar -> value.toInstant()
            is ZonedDateTime -> value.toInstant()
            is LocalDateTime -> value.atZone(timeZone).toInstant()
            is LocalDate -> value.atStartOfDay(timeZone).toInstant()
            is Instant -> value
            else -> null
        }
    }
}
