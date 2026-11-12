package app.ioutracker.ui.counterparty

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.ioutracker.MainApplication
import app.ioutracker.data.CounterpartyEntity
import app.ioutracker.data.CurrencyDao
import app.ioutracker.data.LoanDao
import app.ioutracker.data.LoanSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CounterpartyUiState(
    val counterparty: CounterpartyEntity? = null,
    val loans: List<LoanSummary> = emptyList(),
    val currencyCode: String? = null,
    val loading: Boolean = true,
)

class CounterpartyViewModel(
    counterpartyId: String,
    loans: LoanDao,
    currency: CurrencyDao,
) : ViewModel() {
    val state: StateFlow<CounterpartyUiState> =
        combine(
            loans.observeCounterparty(counterpartyId),
            loans.observeLoansOf(counterpartyId),
            currency.observeCode(),
        ) { counterparty, loansOf, code ->
            CounterpartyUiState(counterparty, loansOf, code, loading = false)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CounterpartyUiState())

    companion object {
        /** A factory per id: a `ViewModelStore` keys on the class, and the id is a constructor argument. */
        fun factoryFor(counterpartyId: String): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[APPLICATION_KEY] as MainApplication
                    CounterpartyViewModel(counterpartyId, app.container.loans, app.container.currency)
                }
            }
    }
}
