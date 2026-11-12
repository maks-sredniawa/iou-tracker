package app.ioutracker.ui.loan

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.TimeZone

class PickerDayTest {
    private val original = TimeZone.getDefault()

    @After
    fun restoreZone() = TimeZone.setDefault(original)

    private val days =
        listOf(
            LocalDate.of(2026, 11, 12),
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 3, 29),
            LocalDate.of(2024, 2, 29),
            LocalDate.of(1999, 12, 31),
        )

    private val zones = listOf("America/Los_Angeles", "Pacific/Pago_Pago", "UTC", "Europe/Warsaw", "Pacific/Kiritimati")

    @Test
    fun `the picker's UTC midnight is the same day in every default zone`() {
        zones.forEach { zone ->
            TimeZone.setDefault(TimeZone.getTimeZone(zone))
            days.forEach { day ->
                val utcMidnight = day.toEpochDay() * 86_400_000L
                assertEquals("$day under $zone", day, dayFromPickerMillis(utcMidnight))
            }
        }
    }

    @Test
    fun `a day survives the round trip through the picker`() {
        zones.forEach { zone ->
            TimeZone.setDefault(TimeZone.getTimeZone(zone))
            days.forEach { assertEquals("$it under $zone", it, dayFromPickerMillis(pickerMillisOf(it))) }
        }
    }

    @Test
    fun `the millis handed to the picker are UTC midnight`() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Warsaw"))
        assertEquals(1_794_441_600_000L, pickerMillisOf(LocalDate.of(2026, 11, 12)))
    }
}
