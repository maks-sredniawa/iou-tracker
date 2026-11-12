package app.ioutracker.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

/**
 * [minorUnits] in the Currency [currencyCode], in [locale]'s format. Without a Currency the number
 * is shown as the bare count of minor units, since there is no scale to put it at.
 */
fun formatAmount(
    minorUnits: Long,
    currencyCode: String?,
    locale: Locale,
): String {
    val currency = currencyCode?.let { runCatching { Currency.getInstance(it) }.getOrNull() }
    val digits = currency?.defaultFractionDigits ?: -1
    if (currency == null || digits < 0) return NumberFormat.getIntegerInstance(locale).format(minorUnits)
    return NumberFormat
        .getCurrencyInstance(locale)
        .apply {
            this.currency = currency
            minimumFractionDigits = digits
            maximumFractionDigits = digits
        }.format(BigDecimal.valueOf(minorUnits, digits))
}

/** The in-app language (ADR-0004), which is what formatting what the owner reads goes through (ADR-0008). */
@Composable
@ReadOnlyComposable
fun appLocale(): Locale = LocalConfiguration.current.locales[0]

fun formatDay(
    day: LocalDate,
    locale: Locale,
): String = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(day)
