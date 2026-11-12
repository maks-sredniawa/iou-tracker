package app.ioutracker.data

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

/**
 * How non-primitive types cross into SQLite.
 *
 * **Two rules here are load-bearing, and both are about not being able to change your mind later.**
 *
 * 1. **An `Instant` is stored as epoch millis**, so the column is a number SQLite can sort and
 *    range-query. A formatted string would sort wrong across timezones and need parsing to compare.
 *    A `LocalDate` has no zone to sort wrong across, so it is ISO-8601 text (`2026-11-12`), which
 *    sorts as a date and reads as one in a pulled database.
 *
 * 2. **An enum is stored by `name`, never by `ordinal`.** An ordinal is the enum's *position*, so
 *    inserting a new constant anywhere but the end silently rewrites the meaning of every row
 *    already on disk — a data-loss bug with no error and no migration to hang a fix on. Storing the
 *    name costs a few bytes per row and makes adding a constant free. Renaming one then becomes the
 *    breaking change, which is the right trade: a rename is a decision, a reorder is an accident.
 *
 * Kotlin note: `enumValueOf<T>(name)` throws on an unknown name. That is deliberate — a value in
 * the database that this build has never heard of means the file is from a newer version, which the
 * schema gate should have caught before Room ever opened it.
 */
class Converters {
    @TypeConverter
    fun instantToEpochMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun epochMillisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun localDateToIso(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun isoToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun directionToName(value: Direction?): String? = value?.name

    @TypeConverter
    fun nameToDirection(value: String?): Direction? = value?.let { enumValueOf<Direction>(it) }

    @TypeConverter
    fun counterpartyKindToName(value: CounterpartyKind?): String? = value?.name

    @TypeConverter
    fun nameToCounterpartyKind(value: String?): CounterpartyKind? = value?.let { enumValueOf<CounterpartyKind>(it) }

    @TypeConverter
    fun themeModeToName(value: ThemeMode?): String? = value?.name

    @TypeConverter
    fun nameToThemeMode(value: String?): ThemeMode? = value?.let { enumValueOf<ThemeMode>(it) }
}
