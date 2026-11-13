package app.ioutracker.ui.loan

import android.database.SQLException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.ioutracker.MainApplication
import app.ioutracker.data.CounterpartyBalance
import app.ioutracker.data.CounterpartyEntity
import app.ioutracker.data.CounterpartyKind
import app.ioutracker.data.CurrencyDao
import app.ioutracker.data.Direction
import app.ioutracker.data.LoanDao
import app.ioutracker.data.LoanEntity
import app.ioutracker.ui.common.ParsedAmount
import app.ioutracker.ui.common.parseAmount
import app.ioutracker.ui.currency.fractionDigitsOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Add Loan's fields in screen order, which is the order Save moves focus through. */
enum class AddLoanField {
    DIRECTION,
    WHO,
    AMOUNT,
}

data class AddLoanUiState(
    val day: LocalDate,
    val loading: Boolean = true,
    /** Null while no Currency this phone can resolve is stored: the form cannot parse an Amount. */
    val currencyCode: String? = null,
    val who: String = "",
    val match: CounterpartyBalance? = null,
    val suggestions: List<CounterpartyBalance> = emptyList(),
    val newKind: CounterpartyKind = CounterpartyKind.PERSON,
    val direction: Direction? = null,
    val amount: String = "",
    val parsed: ParsedAmount = ParsedAmount.Empty,
    val revealed: Set<AddLoanField> = emptySet(),
    val edited: Boolean = false,
    val saving: Boolean = false,
    val failed: Boolean = false,
    val saved: Boolean = false,
    val focus: AddLoanField? = null,
) {
    val kind: CounterpartyKind get() = match?.kind ?: newKind

    val invalid: List<AddLoanField>
        get() =
            listOfNotNull(
                AddLoanField.DIRECTION.takeIf { direction == null },
                AddLoanField.WHO.takeIf { storedCounterpartyName(who).isEmpty() },
                AddLoanField.AMOUNT.takeIf { parsed !is ParsedAmount.Valid },
            )

    fun showsError(field: AddLoanField): Boolean = field in revealed && field in invalid
}

class AddLoanViewModel(
    counterpartyId: String?,
    private val savedState: SavedStateHandle,
    private val loans: LoanDao,
    currency: CurrencyDao,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    private data class Typed(
        val who: String?,
        val kind: CounterpartyKind,
        val direction: Direction?,
        val amount: String,
        val day: LocalDate,
    )

    private data class Session(
        val revealed: Set<AddLoanField> = emptySet(),
        val saving: Boolean = false,
        val failed: Boolean = false,
        val saved: Boolean = false,
        val focus: AddLoanField? = null,
    )

    private val session = MutableStateFlow(Session())

    init {
        if (OPENED_ON !in savedState) savedState[OPENED_ON] = today().toString()
        if (DAY !in savedState) savedState[DAY] = savedState.get<String>(OPENED_ON)
        if (WHO !in savedState) {
            if (counterpartyId == null) {
                savedState[INITIAL_WHO] = ""
                savedState[WHO] = ""
            } else {
                viewModelScope.launch {
                    val name =
                        loans
                            .observeCounterparty(counterpartyId)
                            .first()
                            ?.name
                            .orEmpty()
                    savedState[INITIAL_WHO] = name
                    savedState[WHO] = name
                }
            }
        }
    }

    private val typed =
        combine(
            savedState.getStateFlow<String?>(WHO, null),
            savedState.getStateFlow(KIND, CounterpartyKind.PERSON.name),
            savedState.getStateFlow<String?>(DIRECTION, null),
            savedState.getStateFlow(AMOUNT, ""),
            savedState.getStateFlow(DAY, today().toString()),
        ) { who, kind, direction, amount, day ->
            Typed(
                who = who,
                kind = enumValueOf(kind),
                direction = direction?.let { enumValueOf<Direction>(it) },
                amount = amount,
                day = LocalDate.parse(day),
            )
        }

    val state: StateFlow<AddLoanUiState> =
        combine(typed, loans.observeBalances(), currency.observeCode(), session) { typed, balances, code, session ->
            val digits = code?.let(::fractionDigitsOf)
            val who = typed.who.orEmpty()
            AddLoanUiState(
                day = typed.day,
                loading = typed.who == null,
                currencyCode = code.takeIf { digits != null },
                who = who,
                match = matchCounterparty(who, balances),
                suggestions = suggestionsFor(who, balances),
                newKind = typed.kind,
                direction = typed.direction,
                amount = typed.amount,
                parsed = digits?.let { parseAmount(typed.amount, it) } ?: ParsedAmount.Empty,
                revealed = session.revealed,
                edited =
                    typed.amount.isNotEmpty() ||
                        typed.direction != null ||
                        typed.kind != CounterpartyKind.PERSON ||
                        who != savedState.get<String>(INITIAL_WHO) ||
                        typed.day.toString() != savedState.get<String>(OPENED_ON),
                saving = session.saving,
                failed = session.failed,
                saved = session.saved,
                focus = session.focus,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, AddLoanUiState(day = today()))

    fun setWho(text: String) = typedChanged(AddLoanField.WHO) { savedState[WHO] = text }

    fun setKind(kind: CounterpartyKind) = typedChanged(null) { savedState[KIND] = kind.name }

    fun setDirection(direction: Direction) = typedChanged(null) { savedState[DIRECTION] = direction.name }

    fun setAmount(text: String) = typedChanged(AddLoanField.AMOUNT) { savedState[AMOUNT] = text }

    /** No future day: the Day is when a Loan was made. */
    fun setDay(day: LocalDate) {
        if (day.isAfter(today())) return
        typedChanged(null) { savedState[DAY] = day.toString() }
    }

    /** A field's error shows once the owner leaves it, never while they are typing in it. */
    fun leave(field: AddLoanField) = session.update { it.copy(revealed = it.revealed + field) }

    fun focusHandled() = session.update { it.copy(focus = null) }

    fun save() {
        val current = state.value
        if (current.loading || current.saved || session.value.saving) return
        val code = current.currencyCode ?: return
        val invalid = current.invalid
        if (invalid.isNotEmpty()) {
            session.update { it.copy(revealed = it.revealed + invalid, focus = invalid.first(), failed = false) }
            return
        }
        session.update { it.copy(saving = true, failed = false) }

        val match = current.match
        val newCounterparty =
            if (match ==
                null
            ) {
                CounterpartyEntity(name = storedCounterpartyName(current.who), kind = current.newKind)
            } else {
                null
            }
        val loan =
            LoanEntity(
                counterpartyId = match?.id ?: newCounterparty!!.id,
                direction = current.direction!!,
                amount = (current.parsed as ParsedAmount.Valid).minorUnits,
                day = current.day,
            )
        viewModelScope.launch {
            val written =
                try {
                    loans.addLoan(newCounterparty, loan, parsedAgainst = code)
                } catch (_: SQLException) {
                    false
                }
            session.update { it.copy(saving = false, saved = written, failed = !written) }
        }
    }

    private inline fun typedChanged(
        hides: AddLoanField?,
        write: () -> Unit,
    ) {
        write()
        session.update { it.copy(revealed = if (hides == null) it.revealed else it.revealed - hides, failed = false) }
    }

    companion object {
        private const val WHO = "who"
        private const val INITIAL_WHO = "initialWho"
        private const val KIND = "kind"
        private const val DIRECTION = "direction"
        private const val AMOUNT = "amount"
        private const val DAY = "day"
        private const val OPENED_ON = "openedOn"

        fun factoryFor(counterpartyId: String?): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[APPLICATION_KEY] as MainApplication
                    AddLoanViewModel(
                        counterpartyId,
                        createSavedStateHandle(),
                        app.container.loans,
                        app.container.currency,
                    )
                }
            }
    }
}
