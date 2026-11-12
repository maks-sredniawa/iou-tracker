package app.ioutracker.ui.loan

import app.ioutracker.data.CounterpartyBalance

/** [typed] trimmed, with every inner run of white space collapsed to one space: the name as stored. */
fun counterpartyName(typed: String): String =
    buildString {
        var pendingSpace = false
        for (char in typed.trim()) {
            if (char.isWhitespace()) {
                pendingSpace = true
                continue
            }
            if (pendingSpace) append(' ')
            pendingSpace = false
            append(char)
        }
    }

/**
 * The existing Counterparty [typed] names, ignoring case, or null for a new one. Matched in Kotlin
 * rather than SQL because SQLite's `NOCASE` folds ASCII only, and `Łukasz` and `łukasz` would
 * become two Balances (ADR-0009).
 */
fun matchCounterparty(
    typed: String,
    existing: List<CounterpartyBalance>,
): CounterpartyBalance? {
    val name = counterpartyName(typed)
    if (name.isEmpty()) return null
    return existing.firstOrNull { counterpartyName(it.name).equals(name, ignoreCase = true) }
}
