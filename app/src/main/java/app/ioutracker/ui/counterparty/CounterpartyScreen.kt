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
import app.ioutracker.data.CounterpartyKind
import app.ioutracker.theme.Spacing
import app.ioutracker.ui.appViewModelExtras
import app.ioutracker.ui.common.AddLoanButton
import app.ioutracker.ui.common.AddLoanButtonClearance
import app.ioutracker.ui.common.CounterpartyIcon
import app.ioutracker.ui.common.DetailScaffold
import app.ioutracker.ui.common.EmptyState
import app.ioutracker.ui.common.LoanRow

@Composable
fun CounterpartyScreen(
    counterpartyId: String,
    onBack: () -> Unit,
    onAddLoan: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CounterpartyViewModel =
        viewModel(factory = CounterpartyViewModel.factoryFor(counterpartyId), extras = appViewModelExtras()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val counterparty = state.counterparty
    DetailScaffold(
        title = counterparty?.name.orEmpty(),
        onBack = onBack,
        modifier = modifier,
        titleIcon =
            if (counterparty?.kind == CounterpartyKind.POT) {
                { CounterpartyIcon(counterparty.kind) }
            } else {
                null
            },
        floatingActionButton = { if (state.loans.isNotEmpty()) AddLoanButton(onAddLoan) },
    ) { contentModifier ->
        when {
            state.loading -> Unit

            state.loans.isEmpty() ->
                EmptyState(
                    message = stringResource(R.string.counterparty_no_loans),
                    modifier = contentModifier,
                    actionLabel = stringResource(R.string.home_add_loan),
                    onAction = onAddLoan,
                )

            else ->
                LazyColumn(
                    modifier = contentModifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            start = Spacing.base,
                            top = Spacing.base,
                            end = Spacing.base,
                            bottom = AddLoanButtonClearance,
                        ),
                ) {
                    items(state.loans, key = { it.id }) { loan -> LoanRow(loan, state.currencyCode) }
                }
        }
    }
}
