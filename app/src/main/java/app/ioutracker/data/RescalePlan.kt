package app.ioutracker.data

/** One stored minor-unit value, a Loan's Amount or one of its Repayments, under the Loan it belongs to. */
data class StoredValue(
    val loanId: String,
    val minorUnits: Long,
)

/** What a Currency change does to every stored value (ADR-0008). */
sealed interface RescalePlan {
    data object Relabel : RescalePlan

    data class Multiply(
        val factor: Long,
    ) : RescalePlan

    data class Divide(
        val factor: Long,
    ) : RescalePlan

    /** [loanIds] are distinct, in the order their first value appeared in the input. */
    data class Refused(
        val loanIds: List<String>,
    ) : RescalePlan
}

fun planRescale(
    oldDigits: Int,
    newDigits: Int,
    values: List<StoredValue>,
): RescalePlan {
    require(oldDigits >= 0 && newDigits >= 0) { "fraction digits cannot be negative: $oldDigits, $newDigits" }
    if (newDigits == oldDigits) return RescalePlan.Relabel
    if (newDigits > oldDigits) return RescalePlan.Multiply(powerOfTen(newDigits - oldDigits))
    val factor = powerOfTen(oldDigits - newDigits)
    val blocking = values.filter { it.minorUnits % factor != 0L }.map { it.loanId }.distinct()
    return if (blocking.isEmpty()) RescalePlan.Divide(factor) else RescalePlan.Refused(blocking)
}

internal fun powerOfTen(exponent: Int): Long = (1..exponent).fold(1L) { acc, _ -> Math.multiplyExact(acc, 10L) }
