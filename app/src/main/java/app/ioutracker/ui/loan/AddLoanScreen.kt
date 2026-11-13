package app.ioutracker.ui.loan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.ioutracker.R
import app.ioutracker.data.CounterpartyKind
import app.ioutracker.data.Direction
import app.ioutracker.theme.Spacing
import app.ioutracker.ui.appViewModelExtras
import app.ioutracker.ui.common.CounterpartyIcon
import app.ioutracker.ui.common.DetailScaffold
import app.ioutracker.ui.common.EmptyState
import app.ioutracker.ui.common.ParsedAmount
import app.ioutracker.ui.common.appLocale
import app.ioutracker.ui.common.formatAmount
import app.ioutracker.ui.common.formatDay
import app.ioutracker.ui.currency.fractionDigitsOf
import java.time.LocalDate

@Composable
fun AddLoanScreen(
    counterpartyId: String?,
    onClose: () -> Unit,
    onChooseCurrency: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddLoanViewModel =
        viewModel(factory = AddLoanViewModel.factoryFor(counterpartyId), extras = appViewModelExtras()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var askingToDiscard by rememberSaveable { mutableStateOf(false) }
    val leave = { if (state.edited && !state.saved) askingToDiscard = true else onClose() }

    LaunchedEffect(state.saved) { if (state.saved) onClose() }
    BackHandler(enabled = state.edited && !state.saved) { askingToDiscard = true }

    val formShown = !state.loading && state.currencyCode != null
    DetailScaffold(
        title = stringResource(R.string.add_loan_title),
        onBack = leave,
        modifier = modifier,
        bottomBar = { if (formShown) SaveBar(state, onSave = viewModel::save) },
    ) { contentModifier ->
        when {
            state.loading -> Unit

            state.currencyCode == null ->
                EmptyState(
                    message = stringResource(R.string.add_loan_no_currency),
                    modifier = contentModifier,
                    actionLabel = stringResource(R.string.add_loan_choose_currency),
                    onAction = onChooseCurrency,
                )

            else -> AddLoanForm(state, viewModel, startInAmount = counterpartyId != null, modifier = contentModifier)
        }
    }

    if (askingToDiscard) {
        AlertDialog(
            onDismissRequest = { askingToDiscard = false },
            title = { Text(stringResource(R.string.add_loan_discard_title)) },
            text = { Text(stringResource(R.string.add_loan_discard_body)) },
            confirmButton = {
                TextButton(onClick = {
                    askingToDiscard = false
                    onClose()
                }) { Text(stringResource(R.string.add_loan_discard_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { askingToDiscard = false }) {
                    Text(stringResource(R.string.add_loan_discard_keep))
                }
            },
        )
    }
}

@Composable
private fun AddLoanForm(
    state: AddLoanUiState,
    viewModel: AddLoanViewModel,
    startInAmount: Boolean,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val who = remember { FocusRequester() }
    val amount = remember { FocusRequester() }
    var focusedOnOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!focusedOnOpen) {
            (if (startInAmount) amount else who).requestFocus()
            focusedOnOpen = true
        }
    }
    LaunchedEffect(state.focus) {
        when (state.focus) {
            null -> return@LaunchedEffect
            AddLoanField.DIRECTION -> {
                focusManager.clearFocus()
                scroll.animateScrollTo(0)
            }
            AddLoanField.WHO -> who.requestFocus()
            AddLoanField.AMOUNT -> amount.requestFocus()
        }
        viewModel.focusHandled()
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(Spacing.base),
        verticalArrangement = Arrangement.spacedBy(Spacing.base),
    ) {
        DirectionChoice(state, onChoose = viewModel::setDirection)
        WhoField(state, viewModel, Modifier.focusRequester(who))
        AmountField(state, viewModel, Modifier.focusRequester(amount))
        DayRow(state.day, onPick = viewModel::setDay)
    }
}

@Composable
private fun DirectionChoice(
    state: AddLoanUiState,
    onChoose: (Direction) -> Unit,
) {
    val error = state.showsError(AddLoanField.DIRECTION)
    val errorText = stringResource(R.string.add_loan_direction_empty)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.hair)) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth().semantics { if (error) error(errorText) },
        ) {
            Direction.entries.forEachIndexed { index, direction ->
                val lent = direction == Direction.LENT
                SegmentedButton(
                    selected = state.direction == direction,
                    onClick = { onChoose(direction) },
                    shape = SegmentedButtonDefaults.itemShape(index, Direction.entries.size),
                    colors =
                        SegmentedButtonDefaults.colors(
                            activeContainerColor =
                                if (lent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                            activeContentColor =
                                if (lent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onTertiary,
                        ),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.hair)) {
                        Text(if (lent) "↗" else "↙", modifier = Modifier.clearAndSetSemantics {})
                        Text(stringResource(if (lent) R.string.direction_lent else R.string.direction_borrowed))
                    }
                }
            }
        }
        if (error) {
            Text(errorText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        if (state.kind == CounterpartyKind.POT) {
            Text(
                stringResource(R.string.add_loan_direction_pot_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WhoField(
    state: AddLoanUiState,
    viewModel: AddLoanViewModel,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val error = state.showsError(AddLoanField.WHO)

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.tight)) {
        ExposedDropdownMenuBox(
            expanded = menuOpen && focused && state.suggestions.isNotEmpty(),
            onExpandedChange = { menuOpen = it },
        ) {
            OutlinedTextField(
                value = state.who,
                onValueChange = {
                    viewModel.setWho(it)
                    menuOpen = true
                },
                label = { Text(stringResource(R.string.add_loan_who)) },
                leadingIcon = state.match?.let { match -> { CounterpartyIcon(match.kind) } },
                isError = error,
                supportingText = if (error) ({ Text(stringResource(R.string.add_loan_who_empty)) }) else null,
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier =
                    modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                        .onFocusChanged {
                            if (focused && !it.isFocused) viewModel.leave(AddLoanField.WHO)
                            focused = it.isFocused
                        },
            )
            ExposedDropdownMenu(
                expanded = menuOpen && focused && state.suggestions.isNotEmpty(),
                onDismissRequest = { menuOpen = false },
            ) {
                for (suggestion in state.suggestions) {
                    DropdownMenuItem(
                        text = { Text(suggestion.name) },
                        leadingIcon = { CounterpartyIcon(suggestion.kind) },
                        onClick = {
                            viewModel.setWho(suggestion.name)
                            menuOpen = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
        if (state.match == null && state.who.isNotBlank()) {
            Row(modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Spacing.tight)) {
                for (kind in CounterpartyKind.entries) {
                    FilterChip(
                        selected = state.newKind == kind,
                        onClick = { viewModel.setKind(kind) },
                        label = {
                            Text(
                                stringResource(
                                    if (kind ==
                                        CounterpartyKind.POT
                                    ) {
                                        R.string.add_loan_kind_pot
                                    } else {
                                        R.string.add_loan_kind_person
                                    },
                                ),
                            )
                        },
                        leadingIcon = { CounterpartyIcon(kind) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AmountField(
    state: AddLoanUiState,
    viewModel: AddLoanViewModel,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val error = state.showsError(AddLoanField.AMOUNT)
    val locale = appLocale()
    val code = state.currencyCode.orEmpty()
    val supporting: String? =
        when {
            error -> amountError(state.parsed, state.amount, code)
            state.parsed is ParsedAmount.Valid -> formatAmount(state.parsed.minorUnits, code, locale)
            else -> null
        }

    OutlinedTextField(
        value = state.amount,
        onValueChange = viewModel::setAmount,
        label = { Text(stringResource(R.string.add_loan_amount)) },
        suffix = { Text(code) },
        isError = error,
        supportingText = supporting?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        modifier =
            modifier
                .fillMaxWidth()
                .onFocusChanged {
                    if (focused && !it.isFocused) viewModel.leave(AddLoanField.AMOUNT)
                    focused = it.isFocused
                },
    )
}

@Composable
private fun amountError(
    parsed: ParsedAmount,
    typed: String,
    code: String,
): String =
    when (parsed) {
        ParsedAmount.Empty -> stringResource(R.string.add_loan_amount_empty)
        ParsedAmount.Unreadable ->
            stringResource(R.string.add_loan_amount_unreadable, amountExample(fractionDigitsOf(code) ?: 0, appLocale()))
        is ParsedAmount.Ambiguous -> {
            val forms = ambiguityForms(typed)
            stringResource(R.string.add_loan_amount_ambiguous, typed.trim(), forms.asFraction, forms.asWhole)
        }
        is ParsedAmount.TooManyDecimals ->
            if (parsed.allowed == 0) {
                stringResource(R.string.add_loan_amount_whole)
            } else {
                pluralStringResource(R.plurals.add_loan_amount_decimals, parsed.allowed, parsed.allowed)
            }
        ParsedAmount.Zero -> stringResource(R.string.add_loan_amount_zero)
        ParsedAmount.TooLarge -> stringResource(R.string.add_loan_amount_too_large)
        is ParsedAmount.Valid -> ""
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayRow(
    day: LocalDate,
    onPick: (LocalDate) -> Unit,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { picking = true }
                .padding(vertical = Spacing.snug),
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.add_loan_day),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(formatDay(day, appLocale()), style = MaterialTheme.typography.bodyLarge)
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (picking) {
        val today = remember { LocalDate.now() }
        val pickerState =
            rememberDatePickerState(
                initialSelectedDateMillis = pickerMillisOf(day),
                selectableDates =
                    object : SelectableDates {
                        override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= pickerMillisOf(today)

                        override fun isSelectableYear(year: Int) = year <= today.year
                    },
            )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onPick(dayFromPickerMillis(it)) }
                    picking = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { picking = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun SaveBar(
    state: AddLoanUiState,
    onSave: () -> Unit,
) {
    Surface(modifier = Modifier.imePadding()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.base),
            verticalArrangement = Arrangement.spacedBy(Spacing.tight),
        ) {
            if (state.failed) {
                Text(
                    stringResource(R.string.add_loan_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.add_loan_save)) }
        }
    }
}
