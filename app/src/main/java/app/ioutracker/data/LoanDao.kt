package app.ioutracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** A Counterparty and its Balance: Outstanding Lent minus Outstanding Borrowed, in minor units. */
data class CounterpartyBalance(
    val id: String,
    val name: String,
    val kind: CounterpartyKind,
    val balance: Long,
)

/** A Loan as a list shows it, with its Outstanding already taken off its Repayments. */
data class LoanSummary(
    val id: String,
    val direction: Direction,
    val amount: Long,
    val day: LocalDate,
    val dueDate: LocalDate?,
    val outstanding: Long,
)

/**
 * Counterparties, Loans and Repayments. Reads return `Flow`; writes are `suspend`.
 *
 * Inserts keep Room's default `ABORT`: `REPLACE` is a delete and an insert, and deleting a Loan
 * cascades to its Repayments.
 */
@Dao
abstract class LoanDao {
    @Query(
        """
        SELECT c.id, c.name, c.kind,
            COALESCE(SUM(CASE o.direction WHEN 'LENT' THEN o.outstanding ELSE -o.outstanding END), 0) AS balance
        FROM counterparties c
        LEFT JOIN ($OUTSTANDING_PER_LOAN) o ON o.counterpartyId = c.id
        GROUP BY c.id
        ORDER BY c.name COLLATE NOCASE, c.id
        """,
    )
    abstract fun observeBalances(): Flow<List<CounterpartyBalance>>

    @Query("SELECT * FROM counterparties WHERE id = :id")
    abstract fun observeCounterparty(id: String): Flow<CounterpartyEntity?>

    @Query(
        """
        SELECT o.id, o.direction, o.amount, o.day, o.dueDate, o.outstanding
        FROM ($OUTSTANDING_PER_LOAN) o
        WHERE o.counterpartyId = :counterpartyId
        ORDER BY o.day DESC, o.rowid DESC
        """,
    )
    abstract fun observeLoansOf(counterpartyId: String): Flow<List<LoanSummary>>

    @Query("SELECT COUNT(*) FROM loans")
    abstract fun observeLoanCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM loans")
    abstract suspend fun loanCount(): Int

    @Insert
    abstract suspend fun insert(counterparty: CounterpartyEntity)

    @Insert
    abstract suspend fun insert(loan: LoanEntity)

    @Insert
    abstract suspend fun insert(repayment: RepaymentEntity)

    @Delete
    abstract suspend fun delete(counterparty: CounterpartyEntity)

    @Delete
    abstract suspend fun delete(loan: LoanEntity)

    @Query("DELETE FROM repayments")
    protected abstract suspend fun deleteAllRepayments()

    @Query("DELETE FROM loans")
    protected abstract suspend fun deleteAllLoans()

    @Query("DELETE FROM counterparties")
    protected abstract suspend fun deleteAllCounterparties()

    /** Every Counterparty, Loan and Repayment. The Currency stays. */
    @Transaction
    open suspend fun deleteAll() {
        deleteAllRepayments()
        deleteAllLoans()
        deleteAllCounterparties()
    }

    private companion object {
        /** Outstanding is never below zero, whatever the Repayments add up to. */
        const val OUTSTANDING_PER_LOAN =
            """
            SELECT l.rowid AS rowid, l.id, l.counterpartyId, l.direction, l.amount, l.day, l.dueDate,
                MAX(l.amount - COALESCE((SELECT SUM(r.amount) FROM repayments r WHERE r.loanId = l.id), 0), 0)
                    AS outstanding
            FROM loans l
            """
    }
}
