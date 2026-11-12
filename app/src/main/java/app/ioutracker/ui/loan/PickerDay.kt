package app.ioutracker.ui.loan

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Material 3's `DatePicker` reports a day as its UTC midnight. Read in the system zone instead, every
 * pick west of Greenwich would land a day early.
 */
fun dayFromPickerMillis(utcMillis: Long): LocalDate =
    Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()

fun pickerMillisOf(day: LocalDate): Long = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
