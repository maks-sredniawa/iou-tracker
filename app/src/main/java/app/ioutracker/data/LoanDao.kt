package app.ioutracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
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

/** A Loan that blocks a Currency change, with the Counterparty it is with. */
data class BlockingLoan(
    @Embedded val loan: LoanSummary,
    val counterpartyId: String,
    val counterpartyName: String,
    val counterpartyKind: CounterpartyKind,
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

    @Query(
        """
        SELECT loanId, minorUnits FROM (
            SELECT l.id AS loanId, l.amount AS minorUnits, l.day AS day, l.rowid AS loanRowid,
                0 AS isRepayment, 0 AS valueRowid
            FROM loans l
            UNION ALL
            SELECT r.loanId, r.amount, l.day, l.rowid, 1, r.rowid
            FROM repayments r JOIN loans l ON l.id = r.loanId
        )
        ORDER BY day DESC, loanRowid DESC, isRepayment, valueRowid
        """,
    )
    abstract suspend fun storedValues(): List<StoredValue>

    @Query("SELECT amount FROM loans ORDER BY day DESC, rowid DESC LIMIT 1")
    abstract suspend fun newestAmount(): Long?

    @Query(
        """
        SELECT o.id, o.direction, o.amount, o.day, o.dueDate, o.outstanding,
            o.counterpartyId, c.name AS counterpartyName, c.kind AS counterpartyKind
        FROM ($OUTSTANDING_PER_LOAN) o
        JOIN counterparties c ON c.id = o.counterpartyId
        WHERE $BLOCKED_BY_FACTOR
        ORDER BY c.name COLLATE NOCASE, c.id, o.day DESC, o.rowid DESC
        """,
    )
    abstract fun observeBlockingLoans(factor: Long): Flow<List<BlockingLoan>>

    @Query("SELECT COUNT(*) FROM loans o WHERE $BLOCKED_BY_FACTOR")
    protected abstract suspend fun blockingLoanCount(factor: Long): Int

    @Query("SELECT COUNT(*) FROM currency WHERE id = $CURRENCY_ROW_ID AND code IS :code")
    protected abstract suspend fun currencyIs(code: String?): Int

    @Query("UPDATE currency SET code = :code WHERE id = $CURRENCY_ROW_ID")
    protected abstract suspend fun setCurrency(code: String)

    @Query("UPDATE loans SET amount = amount * :factor")
    protected abstract suspend fun multiplyLoans(factor: Long)

    @Query("UPDATE repayments SET amount = amount * :factor")
    protected abstract suspend fun multiplyRepayments(factor: Long)

    @Query("UPDATE loans SET amount = amount / :factor")
    protected abstract suspend fun divideLoans(factor: Long)

    @Query("UPDATE repayments SET amount = amount / :factor")
    protected abstract suspend fun divideRepayments(factor: Long)

    /**
     * Moves every stored value to [toCode] by [plan], or writes nothing and returns false when the
     * stored code is no longer [fromCode] or a divide has gained a blocking value since it was
     * planned. Neither a multiply nor a divide is idempotent, so a stale plan must not run twice.
     */
    @Transaction
    open suspend fun changeCurrency(
        fromCode: String?,
        toCode: String,
        plan: RescalePlan,
    ): Boolean {
        require(plan !is RescalePlan.Refused) { "a refused plan cannot be applied" }
        if (currencyIs(fromCode) == 0) return false
        when (plan) {
            RescalePlan.Relabel -> Unit
            is RescalePlan.Multiply -> {
                multiplyLoans(plan.factor)
                multiplyRepayments(plan.factor)
            }
            is RescalePlan.Divide -> {
                if (blockingLoanCount(plan.factor) > 0) return false
                divideLoans(plan.factor)
                divideRepayments(plan.factor)
            }
            is RescalePlan.Refused -> return false
        }
        setCurrency(toCode)
        return true
    }

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

        /** Over a Loan aliased `o`: its Amount, or one of its Repayments, has a fraction [factor] cannot divide. */
        const val BLOCKED_BY_FACTOR =
            """
            (o.amount % :factor != 0
                OR EXISTS (SELECT 1 FROM repayments r WHERE r.loanId = o.id AND r.amount % :factor != 0))
            """
    }
}
