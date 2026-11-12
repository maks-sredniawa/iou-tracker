package app.ioutracker.ui.counterparty

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.ioutracker.R
import app.ioutracker.data.Direction
import app.ioutracker.data.LoanSummary
import app.ioutracker.theme.Spacing
import app.ioutracker.ui.appViewModelExtras
import app.ioutracker.ui.common.DetailScaffold
import app.ioutracker.ui.common.EmptyState
import app.ioutracker.ui.common.appLocale
import app.ioutracker.ui.common.formatAmount
import app.ioutracker.ui.common.formatDay

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

@Composable
private fun LoanRow(
    loan: LoanSummary,
    currencyCode: String?,
) {
    val locale = appLocale()
    val lent = loan.direction == Direction.LENT
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.tight)) {
        Row(
            modifier = Modifier.padding(Spacing.base),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.base),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                DirectionLabel(loan.direction)
                Text(
                    text = formatDay(loan.day, locale),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = Spacing.tight),
                )
                loan.dueDate?.let { due ->
                    Text(
                        text = stringResource(R.string.loan_due, formatDay(due, locale)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val progress =
                    when (loan.outstanding) {
                        0L -> stringResource(R.string.loan_settled)
                        loan.amount -> null
                        else ->
                            stringResource(
                                R.string.loan_outstanding,
                                formatAmount(loan.outstanding, currencyCode, locale),
                            )
                    }
                progress?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = (if (lent) "+" else "−") + formatAmount(loan.amount, currencyCode, locale),
                style = MaterialTheme.typography.titleMedium,
                color = if (lent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

/**
 * Never colour alone: the word, an arrow (up-right out, down-left in), and Lent filled where
 * Borrowed is outlined (docs/phase-0.md, *The palette*).
 */
@Composable
private fun DirectionLabel(direction: Direction) {
    val lent = direction == Direction.LENT
    val tone = if (lent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    val shape = MaterialTheme.shapes.small
    CompositionLocalProvider(LocalContentColor provides if (lent) MaterialTheme.colorScheme.onPrimary else tone) {
        Row(
            modifier =
                (if (lent) Modifier.background(tone, shape) else Modifier.border(1.dp, tone, shape))
                    .padding(horizontal = Spacing.tight, vertical = Spacing.hair),
            horizontalArrangement = Arrangement.spacedBy(Spacing.hair),
        ) {
            Text(
                if (lent) "↗" else "↙",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clearAndSetSemantics {},
            )
            Text(
                text = stringResource(if (lent) R.string.direction_lent else R.string.direction_borrowed),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
