package app.ioutracker.ui.currency

import app.ioutracker.data.RescalePlan
import app.ioutracker.data.SUCCESSORS_OF_WITHDRAWN_CODES
import app.ioutracker.data.StoredValue
import app.ioutracker.data.planRescale
import app.ioutracker.data.powerOfTen
import java.text.Collator
import java.util.Currency
import java.util.Date
import java.util.Locale

/** Leaves out the four-digit units of account, CLF and UYW, and keeps a rescale inside a `Long` (ADR-0008). */
private const val MAX_OFFERED_FRACTION_DIGITS = 3

/**
 * ISO 4217 codes in current use that an older platform's currency data predates (API 26 ships ICU 58),
 * with their fraction digits, so that every phone stores them at the same scale.
 */
private val CODES_OLD_PLATFORMS_LACK =
    mapOf("MRU" to 2, "SLE" to 2, "STN" to 2, "VED" to 2, "VES" to 2, "XCG" to 2, "ZWG" to 2)

/** Codes an older platform still calls current although ISO 4217 withdrew them, and CNH, which ICU lists but ISO never did. */
private val CODES_NOT_IN_USE = SUCCESSORS_OF_WITHDRAWN_CODES.keys + "CNH"

fun fractionDigitsOf(code: String): Int? = correctedFractionDigits(code, platformFractionDigits(code))

internal fun correctedFractionDigits(
    code: String,
    platformDigits: Int?,
): Int? = platformDigits ?: CODES_OLD_PLATFORMS_LACK[code]

private fun platformFractionDigits(code: String): Int? =
    runCatching { Currency.getInstance(code).defaultFractionDigits }.getOrNull()?.takeIf { it >= 0 }

fun currencyName(
    code: String,
    locale: Locale,
): String = runCatching { Currency.getInstance(code).getDisplayName(locale) }.getOrDefault(code)

private val X_CODES_IN_USE = setOf("XAF", "XCD", "XCG", "XOF", "XPF")

private val FUND_CODES = setOf("BOV", "CHE", "CHW", "COU", "MXV", "USN", "UYI")

/**
 * ISO 4217's other X codes are metals, funds, units of account and test codes, which ICU still lists
 * as available, with two digits; [FUND_CODES] are its funds outside X, which CLDR marks as not tender.
 */
fun isOfferedCurrency(
    code: String,
    fractionDigits: Int,
): Boolean =
    fractionDigits in 0..MAX_OFFERED_FRACTION_DIGITS &&
        code !in FUND_CODES &&
        (!code.startsWith("X") || code in X_CODES_IN_USE)

fun currenciesInUse(now: Date = Date()): List<String> =
    correctedCurrencyCodes(
        Currency
            .getAvailableCurrencies()
            .filter { isOfferedCurrency(it.currencyCode, it.defaultFractionDigits) }
            .map { it.currencyCode }
            .filter {
                android.icu.util.Currency
                    .isAvailable(it, now, now)
            },
    )

/** The platform's answer with ADR-0008's corrections: what it wrongly calls current goes, what it lacks comes in. */
internal fun correctedCurrencyCodes(platformCodes: List<String>): List<String> =
    (platformCodes.filterNot { it in CODES_NOT_IN_USE } + CODES_OLD_PLATFORMS_LACK.keys).distinct()

data class CurrencyOption(
    val code: String,
    val name: String,
)

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

/**
 * How a change from the stored [fromCode] to a currency of [newDigits] moves every stored value. No
 * Currency yet relabels; a stored code this platform cannot resolve is refused with no blocking
 * Loan, since relabelling it would rescale every Amount by a guess.
 */
inline fun planCurrencyChange(
    fromCode: String?,
    newDigits: Int,
    storedValues: () -> List<StoredValue>,
): RescalePlan {
    if (fromCode == null) return RescalePlan.Relabel
    val oldDigits = fractionDigitsOf(fromCode) ?: return RescalePlan.Refused(emptyList())
    return planRescale(oldDigits, newDigits, storedValues())
}

/**
 * What a stored value must divide by to fit [toCode]: 1, which blocks nothing, when no digits are
 * lost, and null when this platform cannot resolve the stored [fromCode], so no value can be judged.
 */
fun rescaleDivisor(
    fromCode: String?,
    toCode: String,
): Long? {
    if (fromCode == null) return 1
    val oldDigits = fractionDigitsOf(fromCode) ?: return null
    val newDigits = fractionDigitsOf(toCode) ?: return 1
    return if (oldDigits > newDigits) powerOfTen(oldDigits - newDigits) else 1
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
