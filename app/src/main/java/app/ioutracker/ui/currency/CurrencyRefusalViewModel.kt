package app.ioutracker.ui.currency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.ioutracker.MainApplication
import app.ioutracker.data.BlockingLoan
import app.ioutracker.data.CurrencyDao
import app.ioutracker.data.LoanDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class CurrencyRefusalUiState(
    val toCode: String,
    val currentCode: String? = null,
    val blocking: List<BlockingLoan> = emptyList(),
    val currentUnknown: Boolean = false,
    val loading: Boolean = true,
)

class CurrencyRefusalViewModel(
    toCode: String,
    currency: CurrencyDao,
    loans: LoanDao,
) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<CurrencyRefusalUiState> =
        currency
            .observeCode()
            .flatMapLatest { code ->
                val divisor = rescaleDivisor(code, toCode)
                if (divisor == null) {
                    flowOf(CurrencyRefusalUiState(toCode, code, currentUnknown = true, loading = false))
                } else {
                    loans.observeBlockingLoans(divisor).map { blocking ->
                        CurrencyRefusalUiState(toCode, code, blocking, loading = false)
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CurrencyRefusalUiState(toCode))

    companion object {
        fun factoryFor(toCode: String): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[APPLICATION_KEY] as MainApplication
                    CurrencyRefusalViewModel(toCode, app.container.currency, app.container.loans)
                }
            }
    }
}
