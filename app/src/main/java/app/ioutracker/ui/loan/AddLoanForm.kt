package app.ioutracker.ui.loan

import app.ioutracker.data.CounterpartyBalance
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

/** The two ways to retype an Ambiguous Amount that each read one way only (docs/phase-1.md, section 1). */
data class AmbiguityForms(
    val asFraction: String,
    val asWhole: String,
)

fun ambiguityForms(typed: String): AmbiguityForms {
    val text = typed.trim()
    val markAt = text.indexOfLast { it == ',' || it == '.' }
    return AmbiguityForms(asFraction = text + "0", asWhole = text.replaceRange(markAt, markAt + 1, " "))
}

/** Four whole digits, so its decimal mark can never be read as grouping in any Currency. */
fun amountExample(
    fractionDigits: Int,
    locale: Locale,
): String =
    NumberFormat
        .getNumberInstance(locale)
        .apply {
            isGroupingUsed = false
            minimumFractionDigits = fractionDigits
            maximumFractionDigits = fractionDigits
        }.format(BigDecimal("1250.5"))

/** The existing Counterparties whose names contain [typed], none once it names exactly one. */
fun suggestionsFor(
    typed: String,
    existing: List<CounterpartyBalance>,
): List<CounterpartyBalance> {
    val needle = storedCounterpartyName(typed)
    if (needle.isEmpty()) return emptyList()
    val found = existing.filter { storedCounterpartyName(it.name).contains(needle, ignoreCase = true) }
    val onlyTheMatch = found.size == 1 && matchCounterparty(typed, found) != null
    return if (onlyTheMatch) emptyList() else found
}
