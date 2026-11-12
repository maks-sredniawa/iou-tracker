package app.ioutracker.ui.counterparty

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.ioutracker.R
import app.ioutracker.theme.Spacing
import app.ioutracker.ui.appViewModelExtras
import app.ioutracker.ui.common.DetailScaffold
import app.ioutracker.ui.common.EmptyState
import app.ioutracker.ui.common.LoanRow

@Composable
fun CounterpartyScreen(
    counterpartyId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CounterpartyViewModel =
        viewModel(factory = CounterpartyViewModel.factoryFor(counterpartyId), extras = appViewModelExtras()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    DetailScaffold(
        title = state.counterparty?.name.orEmpty(),
        onBack = onBack,
        modifier = modifier,
    ) { contentModifier ->
        when {
            state.loading -> Unit

            state.loans.isEmpty() ->
                EmptyState(message = stringResource(R.string.counterparty_no_loans), modifier = contentModifier)

            else ->
                LazyColumn(
                    modifier = contentModifier.fillMaxSize(),
                    contentPadding = PaddingValues(Spacing.base),
                ) {
                    items(state.loans, key = { it.id }) { loan -> LoanRow(loan, state.currencyCode) }
                }
        }
    }
}
