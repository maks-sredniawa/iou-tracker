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
import app.ioutracker.data.powerOfTen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class CurrencyRefusalUiState(
    val toCode: String,
    val currentCode: String? = null,
    val blocking: List<BlockingLoan> = emptyList(),
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
                loans.observeBlockingLoans(divisor(code, toCode)).map { blocking ->
                    CurrencyRefusalUiState(toCode, code, blocking, loading = false)
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

/** What a stored value must divide by to fit [toCode]; 1, which blocks nothing, when no digits are lost. */
private fun divisor(
    fromCode: String?,
    toCode: String,
): Long {
    val oldDigits = fromCode?.let(::fractionDigitsOf) ?: return 1
    val newDigits = fractionDigitsOf(toCode) ?: return 1
    return if (oldDigits > newDigits) powerOfTen(oldDigits - newDigits) else 1
}
