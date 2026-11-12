package app.ioutracker.ui.currency

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.ioutracker.R
import app.ioutracker.theme.Spacing
import app.ioutracker.ui.appViewModelExtras
import app.ioutracker.ui.common.DetailScaffold
import app.ioutracker.ui.common.LoanRow
import app.ioutracker.ui.common.appLocale

@Composable
fun CurrencyRefusedScreen(
    toCode: String,
    onBack: () -> Unit,
    onOpenCounterparty: (counterpartyId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CurrencyRefusalViewModel =
        viewModel(factory = CurrencyRefusalViewModel.factoryFor(toCode), extras = appViewModelExtras()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toName = currencyName(toCode, appLocale())

    DetailScaffold(
        title = stringResource(R.string.settings_currency_refused_title),
        onBack = onBack,
        modifier = modifier,
    ) { contentModifier ->
        if (state.loading) return@DetailScaffold
        LazyColumn(
            modifier = contentModifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacing.base),
        ) {
            item {
                Text(
                    text =
                        if (state.currentUnknown) {
                            stringResource(R.string.settings_currency_refused_unknown, state.currentCode.orEmpty())
                        } else {
                            pluralStringResource(
                                R.plurals.settings_currency_refused,
                                state.blocking.size,
                                state.blocking.size,
                                toName,
                            )
                        },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = Spacing.base),
                )
            }
            items(state.blocking, key = { it.loan.id }) { blocking ->
                LoanRow(
                    loan = blocking.loan,
                    currencyCode = state.currentCode,
                    heading = blocking.counterpartyName,
                    onClick = { onOpenCounterparty(blocking.counterpartyId) },
                )
            }
        }
    }
}
