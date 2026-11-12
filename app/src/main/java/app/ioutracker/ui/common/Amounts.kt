package app.ioutracker.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import app.ioutracker.data.powerOfTen
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

sealed interface ParsedAmount {
    data class Valid(
        val minorUnits: Long,
    ) : ParsedAmount

    data object Empty : ParsedAmount

    data object Unreadable : ParsedAmount

    /** Both readings of a lone mark before three digits, in minor units, for the field to offer. */
    data class Ambiguous(
        val asDecimal: Long,
        val asGrouped: Long,
    ) : ParsedAmount

    data class TooManyDecimals(
        val allowed: Int,
    ) : ParsedAmount

    data object Zero : ParsedAmount

    data object TooLarge : ParsedAmount
}

/**
 * The whole part of an Amount, in major units, stays below this. Stated in major units so a rescale
 * cannot push a stored value past it, and small enough that `amount * 1000` fits a `Long` (docs/phase-1.md, section 1).
 */
const val AMOUNT_WHOLE_LIMIT = 1_000_000_000_000L

private val AMOUNT_WHOLE_LIMIT_DIGITS = AMOUNT_WHOLE_LIMIT.toString().length - 1

private const val MAX_FRACTION_DIGITS = 6

/**
 * The owner's typed Amount in minor units at [fractionDigits], by ADR-0008's rule and its phase 1
 * bounds. Independent of every locale on purpose.
 */
fun parseAmount(
    typed: String,
    fractionDigits: Int,
): ParsedAmount {
    require(fractionDigits in 0..MAX_FRACTION_DIGITS) { "unsupported fraction digits: $fractionDigits" }
    val text = typed.trim().map { if (it.isSpaceMark()) ' ' else it }.joinToString("")
    if (text.isEmpty()) return ParsedAmount.Empty
    if (!text.all { it.isAsciiDigit() || it.isSeparator() } || text.none { it.isAsciiDigit() }) {
        return ParsedAmount.Unreadable
    }

    val markAt = text.indexOfLast { it == ',' || it == '.' }
    val whole: String
    val fraction: String?
    if (markAt < 0) {
        whole = text
        fraction = null
    } else {
        val mark = text[markAt]
        val before = text.substring(0, markAt)
        val after = text.substring(markAt + 1)
        val isDecimal =
            when {
                text.count { it == mark } > 1 -> false
                before.any { it.isSeparator() } -> true
                before.isNotEmpty() && before.all { it == '0' } -> true
                !(after.length == 3 && after.all { it.isAsciiDigit() }) -> true
                before.length !in 1..3 -> true
                fractionDigits < 3 -> false
                else -> return ambiguous(before, after, fractionDigits)
            }
        whole = if (isDecimal) before else text
        fraction = if (isDecimal) after else null
    }

    val wholeDigits = ungrouped(whole) ?: return ParsedAmount.Unreadable
    if (fraction != null) {
        if (fraction.isEmpty() || !fraction.all { it.isAsciiDigit() }) return ParsedAmount.Unreadable
        if (fraction.drop(fractionDigits).any { it != '0' }) return ParsedAmount.TooManyDecimals(fractionDigits)
    }
    if (wholeDigits.trimStart('0').length > AMOUNT_WHOLE_LIMIT_DIGITS) return ParsedAmount.TooLarge

    val minorUnits = toMinorUnits(wholeDigits, fraction?.take(fractionDigits).orEmpty(), fractionDigits)
    return if (minorUnits == 0L) ParsedAmount.Zero else ParsedAmount.Valid(minorUnits)
}

private fun ambiguous(
    whole: String,
    threeDigits: String,
    fractionDigits: Int,
) = ParsedAmount.Ambiguous(
    asDecimal = toMinorUnits(whole, threeDigits, fractionDigits),
    asGrouped = toMinorUnits(whole + threeDigits, "", fractionDigits),
)

private fun toMinorUnits(
    wholeDigits: String,
    fraction: String,
    fractionDigits: Int,
): Long {
    val fractionMinor = if (fraction.isEmpty()) 0L else fraction.toLong() * powerOfTen(fractionDigits - fraction.length)
    return wholeDigits.toLong() * powerOfTen(fractionDigits) + fractionMinor
}

/** [whole]'s digits with its grouping removed, or null when the grouping breaks ADR-0008's rule 4. */
private fun ungrouped(whole: String): String? {
    val marks = whole.filterNot { it.isAsciiDigit() }.toSet()
    if (marks.isEmpty()) return whole.ifEmpty { null }
    if (marks.size > 1) return null
    val groups = whole.split(marks.single())
    val wellGrouped = groups.first().length in 1..3 && groups.drop(1).all { it.length == 3 }
    return if (wellGrouped) groups.joinToString("") else null
}

private fun Char.isAsciiDigit() = this in '0'..'9'

private fun Char.isSeparator() = this == ',' || this == '.' || this == ' '

private fun Char.isSpaceMark() = this == ' ' || this == '\u00A0' || this == '\u202F'
