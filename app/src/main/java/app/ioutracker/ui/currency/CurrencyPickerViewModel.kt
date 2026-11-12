package app.ioutracker.ui.currency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.ioutracker.MainApplication
import app.ioutracker.data.CurrencyDao
import app.ioutracker.data.LoanDao
import app.ioutracker.data.RescalePlan
import app.ioutracker.data.systemCurrencyCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CurrencyChangeWarning(
    val fromCode: String?,
    val toCode: String,
    val plan: RescalePlan,
    val before: Long,
    val after: Long,
)

sealed interface CurrencyPickerOutcome {
    data object Changed : CurrencyPickerOutcome

    data class Refused(
        val toCode: String,
    ) : CurrencyPickerOutcome
}

data class CurrencyPickerUiState(
    val currentCode: String? = null,
    val regionCode: String? = null,
    val codes: List<String> = emptyList(),
    val query: String = "",
    val warning: CurrencyChangeWarning? = null,
    val changing: Boolean = false,
    val outcome: CurrencyPickerOutcome? = null,
)

class CurrencyPickerViewModel(
    private val currency: CurrencyDao,
    private val loans: LoanDao,
    regionCode: String?,
) : ViewModel() {
    private val mutableState = MutableStateFlow(CurrencyPickerUiState(regionCode = regionCode))
    val state: StateFlow<CurrencyPickerUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val inUse = withContext(Dispatchers.Default) { currenciesInUse() }
            mutableState.update { it.copy(codes = inUse) }
        }
        viewModelScope.launch {
            currency.observeCode().collect { code -> mutableState.update { it.copy(currentCode = code) } }
        }
    }

    fun setQuery(query: String) = mutableState.update { it.copy(query = query) }

    fun choose(toCode: String) {
        if (state.value.changing || state.value.warning != null) return
        mutableState.update { it.copy(changing = true) }
        viewModelScope.launch { planChange(toCode) }
    }

    fun confirm() {
        val warning = state.value.warning ?: return
        if (state.value.changing) return
        mutableState.update { it.copy(changing = true, warning = null) }
        viewModelScope.launch { change(warning.fromCode, warning.toCode, warning.plan) }
    }

    fun dismissWarning() = mutableState.update { it.copy(warning = null) }

    fun outcomeHandled() = mutableState.update { it.copy(outcome = null) }

    private suspend fun planChange(toCode: String) {
        val fromCode = currency.observeCode().first()
        if (fromCode == toCode) return finish(CurrencyPickerOutcome.Changed)
        val newDigits = fractionDigitsOf(toCode) ?: return finish(null)
        val plan = planCurrencyChange(fromCode, newDigits) { loans.storedValues() }
        if (plan is RescalePlan.Refused) return finish(CurrencyPickerOutcome.Refused(toCode))
        val newest = loans.newestAmount()
        if (newest == null) return change(fromCode, toCode, plan)
        val warning = CurrencyChangeWarning(fromCode, toCode, plan, newest, rescaled(newest, plan))
        mutableState.update { it.copy(changing = false, warning = warning) }
    }

    /** A stale plan writes nothing, so the change is planned again from the code stored now. */
    private suspend fun change(
        fromCode: String?,
        toCode: String,
        plan: RescalePlan,
    ) {
        if (loans.changeCurrency(fromCode, toCode, plan)) finish(CurrencyPickerOutcome.Changed) else planChange(toCode)
    }

    private fun finish(outcome: CurrencyPickerOutcome?) =
        mutableState.update {
            it.copy(changing = false, outcome = outcome)
        }

    companion object {
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[APPLICATION_KEY] as MainApplication
                    CurrencyPickerViewModel(app.container.currency, app.container.loans, app.systemCurrencyCode())
                }
            }
    }
}
