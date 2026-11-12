package app.ioutracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.ioutracker.MainApplication
import app.ioutracker.data.AppPreferences
import app.ioutracker.data.CounterpartyBalance
import app.ioutracker.data.CurrencyDao
import app.ioutracker.data.LoanDao
import app.ioutracker.tip.TipJar
import app.ioutracker.tip.tipAskDue
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

data class HomeUiState(
    val counterparties: List<CounterpartyBalance> = emptyList(),
    val currencyCode: String? = null,
    val loanCount: Int = 0,
    /** Starts true so the empty state never flashes before the first read lands. */
    val loading: Boolean = true,
    /** The tip's one ask (ADR-0007). False wherever Play will not sell a tip. */
    val showTipAsk: Boolean = false,
)

class HomeViewModel(
    loans: LoanDao,
    currency: CurrencyDao,
    private val preferences: AppPreferences,
    tipJar: TipJar,
    installedAt: Instant,
) : ViewModel() {
    val state: StateFlow<HomeUiState> =
        combine(loans.observeBalances(), currency.observeCode(), loans.observeLoanCount()) { balances, code, count ->
            HomeUiState(counterparties = balances, currencyCode = code, loanCount = count, loading = false)
        }.withTipAsk(preferences.tipAsked, tipJar.offers, installedAt)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        // Prices are asked for only once the ask could be due, saving a product query on every other visit.
        viewModelScope.launch {
            if (tipAskDue(preferences.tipAsked.first(), loans.loanCount(), installedAt, Instant.now())) tipJar.refresh()
        }
    }

    /** Either answer, *Leave a tip* or *No thanks*, ends the ask on this install. */
    fun answerTipAsk() {
        viewModelScope.launch { preferences.markTipAsked() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[APPLICATION_KEY] as MainApplication
                    HomeViewModel(
                        app.container.loans,
                        app.container.currency,
                        app.container.preferences,
                        app.container.tipJar,
                        app.container.installedAt,
                    )
                }
            }
    }
}
