package app.ioutracker.data

import android.content.Context
import androidx.core.app.LocaleManagerCompat
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.Currency
import java.util.Locale

const val CURRENCY_ROW_ID = 1

/**
 * The one Currency every Amount is in (ADR-0008). A table rather than a preference so it travels
 * with the rows it gives meaning to. One row, [CURRENCY_ROW_ID]; [code] is null when the phone's
 * region names no currency.
 */
@Entity(tableName = "currency")
data class CurrencyEntity(
    @PrimaryKey
    val id: Int = CURRENCY_ROW_ID,
    val code: String?,
)

/** The currency of the first locale whose region names one, or null. */
fun currencyCodeForRegions(locales: List<Locale>): String? =
    locales.firstNotNullOfOrNull { locale ->
        if (locale.country.isBlank()) return@firstNotNullOfOrNull null
        runCatching { Currency.getInstance(locale) }
            .getOrNull()
            ?.takeIf { it.defaultFractionDigits >= 0 }
            ?.currencyCode
    }

/** The system's locales, not the in-app language's, which ADR-0004 makes region-less. */
fun Context.systemCurrencyCode(): String? {
    val locales = LocaleManagerCompat.getSystemLocales(this)
    return currencyCodeForRegions((0 until locales.size()).mapNotNull(locales::get))
}

/**
 * Writes the Currency row when the database file is created. The debug build's destructive
 * fallback recreates the tables without calling [onCreate], hence the second override.
 */
class CurrencyRowOnCreate(
    private val systemCurrencyCode: () -> String?,
) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) = insertRow(db)

    override fun onDestructiveMigration(db: SupportSQLiteDatabase) = insertRow(db)

    private fun insertRow(db: SupportSQLiteDatabase) {
        db.execSQL(
            "INSERT OR IGNORE INTO currency (id, code) VALUES (?, ?)",
            arrayOf<Any?>(CURRENCY_ROW_ID, systemCurrencyCode()),
        )
    }
}
