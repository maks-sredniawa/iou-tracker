package app.ioutracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.util.UUID

enum class Direction {
    /** The Counterparty owes the owner. */
    LENT,

    /** The owner owes the Counterparty. */
    BORROWED,
}

/**
 * One sum that changed hands once. [amount] is in minor units of the install's Currency and never
 * changes because of a Repayment (ADR-0008).
 *
 * `RESTRICT` on the Counterparty: deleting one that still has Loans fails rather than taking its
 * Loans with it.
 */
@Entity(
    tableName = "loans",
    foreignKeys = [
        ForeignKey(
            entity = CounterpartyEntity::class,
            parentColumns = ["id"],
            childColumns = ["counterpartyId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("counterpartyId")],
)
data class LoanEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val counterpartyId: String,
    val direction: Direction,
    val amount: Long,
    val day: LocalDate,
    val dueDate: LocalDate? = null,
)
