package app.ioutracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class CounterpartyKind {
    PERSON,
    POT,
}

/** Whoever is on the other side of a Loan: a Person, or a Pot of shared money (ADR-0009). */
@Entity(tableName = "counterparties")
data class CounterpartyEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val kind: CounterpartyKind,
)
