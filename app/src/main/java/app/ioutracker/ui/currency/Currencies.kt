package app.ioutracker.ui.currency

import app.ioutracker.data.RescalePlan
import java.text.Collator
import java.util.Currency
import java.util.Date
import java.util.Locale

/** Leaves out the four-digit units of account, CLF and UYW, and keeps a rescale inside a `Long` (ADR-0008). */
private const val MAX_OFFERED_FRACTION_DIGITS = 3

fun fractionDigitsOf(code: String): Int? =
    runCatching { Currency.getInstance(code).defaultFractionDigits }.getOrNull()?.takeIf { it >= 0 }

fun currencyName(
    code: String,
    locale: Locale,
): String = runCatching { Currency.getInstance(code).getDisplayName(locale) }.getOrDefault(code)

/** The ISO 4217 currencies in use today with zero to three fraction digits. */
fun currenciesInUse(now: Date = Date()): List<String> =
    Currency
        .getAvailableCurrencies()
        .filter { it.defaultFractionDigits in 0..MAX_OFFERED_FRACTION_DIGITS }
        .map { it.currencyCode }
        .filter {
            android.icu.util.Currency
                .isAvailable(it, now, now)
        }

data class CurrencyOption(
    val code: String,
    val name: String,
)

/**
 * The picker's rows: [pinned] first, in that order, then [codes] by localised name. A non-blank
 * [query] keeps the rows whose name or code contains it, ignoring case.
 */
fun currencyOptions(
    codes: List<String>,
    pinned: List<String>,
    query: String,
    locale: Locale,
): List<CurrencyOption> {
    val collator = Collator.getInstance(locale)
    val top = pinned.distinct()
    val rest = codes.filterNot { it in top }.map { CurrencyOption(it, currencyName(it, locale)) }
    val options =
        top.map { CurrencyOption(it, currencyName(it, locale)) } + rest.sortedWith(compareBy(collator) { it.name })
    val needle = query.trim()
    if (needle.isEmpty()) return options
    return options.filter {
        it.name.contains(needle, ignoreCase = true) || it.code.contains(needle, ignoreCase = true)
    }
}

/** [minorUnits] as the same number at the new scale, which is what [plan] does to every stored value. */
fun rescaled(
    minorUnits: Long,
    plan: RescalePlan,
): Long =
    when (plan) {
        RescalePlan.Relabel -> minorUnits
        is RescalePlan.Multiply -> minorUnits * plan.factor
        is RescalePlan.Divide -> minorUnits / plan.factor
        is RescalePlan.Refused -> error("a refused plan rescales nothing")
    }
