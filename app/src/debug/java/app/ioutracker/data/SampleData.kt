package app.ioutracker.data

import java.time.LocalDate

/**
 * Rows to develop and screenshot against. Debug-only, and deterministic, so two runs shoot the same
 * screens. Covers each Balance state: owed to the owner, owed by the owner, and even.
 */
object SampleData {
    suspend fun seed(loans: LoanDao) {
        loans.deleteAll()

        val ola = CounterpartyEntity(name = "Ola", kind = CounterpartyKind.PERSON)
        val savings = CounterpartyEntity(name = "Joint savings", kind = CounterpartyKind.POT)
        val tomek = CounterpartyEntity(name = "Tomek", kind = CounterpartyKind.PERSON)
        listOf(ola, savings, tomek).forEach { loans.insert(it) }

        val lentToOla =
            LoanEntity(
                counterpartyId = ola.id,
                direction = Direction.LENT,
                amount = 5_000,
                day = LocalDate.of(2026, 10, 3),
                dueDate = LocalDate.of(2026, 11, 30),
            )
        val fromSavings =
            LoanEntity(
                counterpartyId = savings.id,
                direction = Direction.BORROWED,
                amount = 30_000,
                day = LocalDate.of(2026, 9, 15),
            )
        listOf(
            lentToOla,
            LoanEntity(
                counterpartyId = ola.id,
                direction = Direction.LENT,
                amount = 1_250,
                day = LocalDate.of(2026, 11, 2),
            ),
            fromSavings,
            LoanEntity(
                counterpartyId = tomek.id,
                direction = Direction.LENT,
                amount = 1_500,
                day = LocalDate.of(2026, 10, 10),
            ),
            LoanEntity(
                counterpartyId = tomek.id,
                direction = Direction.BORROWED,
                amount = 1_500,
                day = LocalDate.of(2026, 10, 24),
            ),
        ).forEach { loans.insert(it) }

        loans.insert(RepaymentEntity(loanId = lentToOla.id, amount = 2_000, day = LocalDate.of(2026, 10, 20)))
        loans.insert(RepaymentEntity(loanId = fromSavings.id, amount = 10_000, day = LocalDate.of(2026, 10, 15)))
    }

    suspend fun wipe(loans: LoanDao) = loans.deleteAll()
}
