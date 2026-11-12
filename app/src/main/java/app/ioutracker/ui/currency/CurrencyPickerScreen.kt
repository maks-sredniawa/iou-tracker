package app.ioutracker.ui.currency

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.ioutracker.R
import app.ioutracker.theme.Spacing
import app.ioutracker.ui.appViewModelExtras
import app.ioutracker.ui.common.DetailScaffold
import app.ioutracker.ui.common.appLocale
import app.ioutracker.ui.common.formatAmount

@Composable
fun CurrencyPickerScreen(
    onBack: () -> Unit,
    onRefused: (toCode: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CurrencyPickerViewModel =
        viewModel(factory = CurrencyPickerViewModel.Factory, extras = appViewModelExtras()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val locale = appLocale()

    LaunchedEffect(state.outcome) {
        when (val outcome = state.outcome) {
            null -> return@LaunchedEffect
            CurrencyPickerOutcome.Changed -> onBack()
            is CurrencyPickerOutcome.Refused -> onRefused(outcome.toCode)
        }
        viewModel.outcomeHandled()
    }

    val options =
        remember(state.codes, state.currentCode, state.regionCode, state.query, locale) {
            currencyOptions(
                codes = state.codes,
                pinned = listOfNotNull(state.currentCode, state.regionCode),
                query = state.query,
                locale = locale,
            )
        }

    DetailScaffold(
        title = stringResource(R.string.settings_currency),
        onBack = onBack,
        modifier = modifier,
    ) { contentModifier ->
        Column(modifier = contentModifier.fillMaxSize()) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                label = { Text(stringResource(R.string.settings_currency_search)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.base, vertical = Spacing.tight),
            )
            LazyColumn(modifier = Modifier.fillMaxSize().selectableGroup()) {
                items(options, key = { it.code }) { option ->
                    CurrencyRow(
                        option = option,
                        selected = option.code == state.currentCode,
                        onClick = { viewModel.choose(option.code) },
                    )
                }
            }
        }
    }

    state.warning?.let { warning ->
        val toName = currencyName(warning.toCode, locale)
        AlertDialog(
            onDismissRequest = viewModel::dismissWarning,
            title = { Text(stringResource(R.string.settings_currency_change_title, toName)) },
            text = {
                Text(
                    stringResource(
                        R.string.settings_currency_change_body,
                        toName,
                        formatAmount(warning.before, warning.fromCode, locale),
                        formatAmount(warning.after, warning.toCode, locale),
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirm, enabled = !state.changing) {
                    Text(stringResource(R.string.settings_currency_change_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissWarning) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun CurrencyRow(
    option: CurrencyOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = Spacing.base, vertical = Spacing.snug),
        horizontalArrangement = Arrangement.spacedBy(Spacing.base),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(option.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            option.code,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
