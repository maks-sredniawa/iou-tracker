package app.ioutracker.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import app.ioutracker.R
import app.ioutracker.data.CounterpartyKind
import app.ioutracker.data.Direction
import app.ioutracker.data.LoanSummary
import app.ioutracker.theme.Spacing

/** A Loan's Counterparty, named above it where the list mixes several. */
data class LoanHeading(
    val name: String,
    val kind: CounterpartyKind,
)

@Composable
fun LoanRow(
    loan: LoanSummary,
    currencyCode: String?,
    modifier: Modifier = Modifier,
    heading: LoanHeading? = null,
    onClick: (() -> Unit)? = null,
) {
    val cardModifier = modifier.fillMaxWidth().padding(bottom = Spacing.tight)
    if (onClick != null) {
        Card(onClick = onClick, modifier = cardModifier) { LoanRowContent(loan, currencyCode, heading) }
    } else {
        Card(modifier = cardModifier) { LoanRowContent(loan, currencyCode, heading) }
    }
}

@Composable
private fun LoanRowContent(
    loan: LoanSummary,
    currencyCode: String?,
    heading: LoanHeading?,
) {
    val locale = appLocale()
    val lent = loan.direction == Direction.LENT
    Row(
        modifier = Modifier.padding(Spacing.base),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.base),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            heading?.let { CounterpartyName(it.name, it.kind, Modifier.padding(bottom = Spacing.tight)) }
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

/** A Counterparty's name as a list shows it, with the Pot's icon beside a Pot's. */
@Composable
fun CounterpartyName(
    name: String,
    kind: CounterpartyKind,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (kind == CounterpartyKind.POT) CounterpartyIcon(kind)
        Text(name, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * Never colour alone: the word, an arrow (up-right out, down-left in), and Lent filled where
 * Borrowed is outlined (docs/phase-0.md, *The palette*).
 */
@Composable
fun DirectionLabel(direction: Direction) {
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
