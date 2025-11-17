package so.clix.models

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Test

class ClixUserPropertyTest {

    @Test
    fun `it should create string property`() {
        // Given
        val name = "test-property"
        val value = "test-value"

        // When
        val property = ClixUserProperty.of(name, value)

        // Then
        assertEquals(name, property.name)
        assertEquals(ClixUserProperty.UserPropertyType.USER_PROPERTY_TYPE_STRING, property.type)
        assertEquals(value, property.value)
    }

    @Test
    fun `it should create boolean property`() {
        // Given
        val name = "test-property"
        val value = true

        // When
        val property = ClixUserProperty.of(name, value)

        // Then
        assertEquals(name, property.name)
        assertEquals(ClixUserProperty.UserPropertyType.USER_PROPERTY_TYPE_BOOLEAN, property.type)
        assertEquals("true", property.value)
    }

    @Test
    fun `it should create number property`() {
        // Given
        val name = "test-property"
        val value = 123

        // When
        val property = ClixUserProperty.of(name, value)

        // Then
        assertEquals(name, property.name)
        assertEquals(ClixUserProperty.UserPropertyType.USER_PROPERTY_TYPE_NUMBER, property.type)
        assertEquals("123", property.value)
    }

    @Test
    fun `it should create datetime property from Date`() {
        // Given
        val name = "test-property"
        val instant = Instant.parse("2024-01-01T10:00:00Z")
        val value = Date.from(instant)

        // When
        val property = ClixUserProperty.of(name, value)

        // Then
        assertEquals(name, property.name)
        assertEquals(ClixUserProperty.UserPropertyType.USER_PROPERTY_TYPE_DATETIME, property.type)
        val expected =
            instant.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        assertEquals(expected, property.value)
    }

    @Test
    fun `it should create datetime property from Calendar`() {
        // Given
        val name = "test-property"
        val instant = Instant.parse("2024-01-01T10:00:00Z")
        val value = Calendar.getInstance().apply { time = Date.from(instant) }

        // When
        val property = ClixUserProperty.of(name, value)

        // Then
        assertEquals(name, property.name)
        assertEquals(ClixUserProperty.UserPropertyType.USER_PROPERTY_TYPE_DATETIME, property.type)
        val expected =
            instant.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        assertEquals(expected, property.value)
    }

    @Test
    fun `it should create datetime property from Instant`() {
        // Given
        val name = "test-property"
        val value = Instant.parse("2024-01-01T10:00:00Z")

        // When
        val property = ClixUserProperty.of(name, value)

        // Then
        assertEquals(name, property.name)
        assertEquals(ClixUserProperty.UserPropertyType.USER_PROPERTY_TYPE_DATETIME, property.type)
        val expected =
            value.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        assertEquals(expected, property.value)
    }

    @Test
    fun `it should create datetime property from ZonedDateTime`() {
        // Given
        val name = "test-property"
        val value = ZonedDateTime.parse("2024-01-01T10:00:00+09:00")

        // When
        val property = ClixUserProperty.of(name, value)

        // Then
        assertEquals(name, property.name)
        assertEquals(ClixUserProperty.UserPropertyType.USER_PROPERTY_TYPE_DATETIME, property.type)
        val expected =
            value
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        assertEquals(expected, property.value)
    }

    @Test
    fun `it should create datetime property from LocalDateTime`() {
        // Given
        val name = "test-property"
        val value = LocalDateTime.parse("2024-01-01T10:00:00")

        // When
        val property = ClixUserProperty.of(name, value)

        // Then
        assertEquals(name, property.name)
        assertEquals(ClixUserProperty.UserPropertyType.USER_PROPERTY_TYPE_DATETIME, property.type)
        val expected =
            value.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        assertEquals(expected, property.value)
    }

    @Test
    fun `it should create datetime property from LocalDate`() {
        // Given
        val name = "test-property"
        val value = LocalDate.parse("2024-01-01")

        // When
        val property = ClixUserProperty.of(name, value)

        // Then
        assertEquals(name, property.name)
        assertEquals(ClixUserProperty.UserPropertyType.USER_PROPERTY_TYPE_DATETIME, property.type)
        val expected =
            value
                .atStartOfDay(ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        assertEquals(expected, property.value)
    }

    @Test
    fun `it should fallback to string for unsupported types`() {
        // Given
        val name = "test-property"
        val value = listOf("item1", "item2")

        // When
        val property = ClixUserProperty.of(name, value)

        // Then
        assertEquals(name, property.name)
        assertEquals(ClixUserProperty.UserPropertyType.USER_PROPERTY_TYPE_STRING, property.type)
        assertEquals(value.toString(), property.value)
    }

    @Test
    fun `it should format datetime with timezone information`() {
        // Given
        val name = "test-property"
        val instant = Instant.parse("2024-01-01T10:00:00Z")

        // When
        val property = ClixUserProperty.of(name, instant)

        // Then
        assertEquals(ClixUserProperty.UserPropertyType.USER_PROPERTY_TYPE_DATETIME, property.type)
        // Should contain timezone offset (+ or -)
        val containsTimezone =
            property.value?.contains("+") == true || property.value?.contains("-") == true
        assertEquals(true, containsTimezone)
    }
}
