package app.ioutracker.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.ioutracker.R
import app.ioutracker.data.CounterpartyBalance
import app.ioutracker.theme.Spacing
import app.ioutracker.ui.appViewModelExtras
import app.ioutracker.ui.common.EmptyState
import app.ioutracker.ui.common.appLocale
import app.ioutracker.ui.common.formatAmount
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenCounterparty: (String) -> Unit,
    onOpenSupport: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel =
        viewModel(factory = HomeViewModel.Factory, extras = appViewModelExtras()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
    ) { insets ->
        when {
            state.loading -> Unit

            state.counterparties.isEmpty() ->
                EmptyState(
                    message = stringResource(R.string.home_empty),
                    modifier = Modifier.padding(insets),
                )

            else ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(insets),
                    contentPadding = PaddingValues(Spacing.base),
                ) {
                    if (state.showTipAsk) {
                        item(key = "tip-ask") {
                            TipAskCard(
                                onTip = {
                                    viewModel.answerTipAsk()
                                    onOpenSupport()
                                },
                                onDecline = viewModel::answerTipAsk,
                            )
                        }
                    }
                    items(state.counterparties, key = { it.id }) { counterparty ->
                        CounterpartyRow(
                            counterparty = counterparty,
                            currencyCode = state.currencyCode,
                            onClick = { onOpenCounterparty(counterparty.id) },
                        )
                    }
                }
        }
    }
}

@Composable
private fun CounterpartyRow(
    counterparty: CounterpartyBalance,
    currencyCode: String?,
    onClick: () -> Unit,
) {
    val balance = counterparty.balance
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.tight)
                .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.base),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.base),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(counterparty.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text =
                        stringResource(
                            when {
                                balance > 0 -> R.string.home_balance_owed_to_you
                                balance < 0 -> R.string.home_balance_you_owe
                                else -> R.string.home_balance_even
                            },
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.hair),
                )
            }
            if (balance != 0L) {
                Text(
                    text =
                        (
                            if (balance >
                                0
                            ) {
                                "+"
                            } else {
                                "−"
                            }
                        ) + formatAmount(balance.absoluteValue, currencyCode, appLocale()),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (balance > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}
