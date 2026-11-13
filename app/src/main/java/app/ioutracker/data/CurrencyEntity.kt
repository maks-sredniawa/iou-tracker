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

/**
 * ISO 4217 codes withdrawn that an older platform's currency data may still call current, each with
 * the code that replaced it (ADR-0008).
 */
internal val SUCCESSORS_OF_WITHDRAWN_CODES =
    mapOf(
        "ANG" to "XCG",
        "BGN" to "EUR",
        "CUC" to "CUP",
        "HRK" to "EUR",
        "MRO" to "MRU",
        "SLL" to "SLE",
        "STD" to "STN",
        "VEF" to "VES",
        "ZWL" to "ZWG",
    )

/** The currency of the first locale whose region names one, past any withdrawn code, or null. */
fun currencyCodeForRegions(
    locales: List<Locale>,
    platformCode: (Locale) -> String? = ::platformCurrencyCode,
): String? =
    locales.firstNotNullOfOrNull { locale ->
        if (locale.country.isBlank()) null else platformCode(locale)?.let { SUCCESSORS_OF_WITHDRAWN_CODES[it] ?: it }
    }

private fun platformCurrencyCode(locale: Locale): String? =
    runCatching { Currency.getInstance(locale) }
        .getOrNull()
        ?.takeIf { it.defaultFractionDigits >= 0 }
        ?.currencyCode

/** The system's locales, not the in-app language's, which ADR-0004 makes region-less. */
fun Context.systemCurrencyCode(): String? {
    val locales = LocaleManagerCompat.getSystemLocales(this)
    return currencyCodeForRegions((0 until locales.size()).mapNotNull(locales::get))
}

/**
 * Writes the Currency row on every open, so a fresh file, a debug destructive recreate and a lost
 * row all get one. `onCreate` would miss the recreate: Room runs `onDestructiveMigration` between
 * dropping the tables and creating them again.
 */
class CurrencyRowOnOpen(
    private val systemCurrencyCode: () -> String?,
) : RoomDatabase.Callback() {
    override fun onOpen(db: SupportSQLiteDatabase) {
        db.execSQL(
            "INSERT OR IGNORE INTO currency (id, code) VALUES (?, ?)",
            arrayOf<Any?>(CURRENCY_ROW_ID, systemCurrencyCode()),
        )
    }
}
