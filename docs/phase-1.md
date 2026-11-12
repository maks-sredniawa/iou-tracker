# Phase 1 — A Loan can be recorded, Lent or Borrowed, with a Person or a Pot, in the owner's Currency

Sequence lives in [`PLAN.md`](PLAN.md); the live worklist lives in [`DOD.md`](DOD.md). This phase
builds on ADR-0008 (one Currency per install, Amounts in minor units: the parser, the rescale, the
refusal) and ADR-0009 (Counterparties typed by hand, a Pot is one of them), and amends ADR-0008 with
two bounds it left open (*Documents this phase amends*). It touches no schema, so ADR-0001 is not
reopened. [`CONTEXT.md`](../CONTEXT.md) is owed nothing unless an answer under *Open for the owner*
changes a word (item 1 adds a sentence to **Counterparty**; the design consult may name the Pot
differently in the UI, which then goes in the table as a UI word beside the code word).

**What closing this phase means.** Today the app opens on Home, which lists Counterparties with their
Balance, and the only way a Loan gets into the database is the debug seeder. After this phase the
owner adds a Loan from the app: types who, picks Lent or Borrowed, types the Amount in either decimal
style, picks the Day, and saves. A new name becomes a Counterparty (a Person or a Pot) in the same
write; a name already used is that Counterparty. Settings shows the Currency and changes it, rescaling
every stored Amount and Repayment exactly, warning first and refusing a change that would lose a
fraction. The difference a device can be asked about: on a release-shaped build with no seeder, a Loan
typed as `100,50` under Polish and as `100.50` under English both read back as 10050 minor units in
the pulled database and as the same Amount on screen, and a change from złoty to yen is refused while
a 12,50 zł Loan exists.

**Where `PLAN.md`'s paragraph is already wrong.**

- *"Home lists Counterparties with their Balance; a Counterparty opens to its Loans"* shipped in
  phase 0 (`ui/home/`, `ui/counterparty/`, `LoanDao.observeBalances` and `observeLoansOf`), and
  `docs/phase-0.md` says so. This phase only adds the way into Add Loan, the empty-state copy that
  points at it, and the Pot's icon that ADR-0009 promises and phase 0 did not draw.
- *"The Currency"* is half shipped: the one-row table, filled from the system region on every open
  (`CurrencyRowOnOpen`), and the formatting that reads it (`ui/common/Amounts.kt`). What is new is
  the Settings surface, the first-time choice when the region named none, and the rescale.
- *"For a new name, its Counterparty"* does not say how a new name becomes a Person or a Pot. It is
  chosen on Add Loan when the name is new (section 2).
- ADR-0008's refusal ends *"the owner edits them or keeps the Currency"*, but editing a Loan is phase
  2. In this phase the refusal can only be accepted (*Known gaps*).
- ADR-0009's *Consequences* promises *"Renaming a Counterparty renames it on every Loan"*, and no
  phase in `PLAN.md` owns renaming, changing a kind, or deleting a Counterparty. *Open for the owner*,
  item 5.
- *"The values a test stands in front of"* names two for this phase. This file adds two more, each a
  pure function that fails in the direction the app exists to prevent (*Tests*); `PLAN.md`'s line is
  reconciled when the phase closes.

---

## 0. What this phase inherits

- **Schema 1, whole.** `CounterpartyEntity` (name, `kind`), `LoanEntity` (Counterparty, Direction,
  Amount in minor units, Day, nullable Due date), `RepaymentEntity`, and the Currency row. No table and
  no column is added (`PLAN.md`, *The door*). `APP_SCHEMA_VERSION` stays 1, and
  `scripts/schema-gate.py` would catch it moving.
- **The Currency row can be null**, when no system locale's region names a currency (ADR-0008,
  `currencyCodeForRegions`). `formatAmount` then shows the bare count of minor units. No Loan can be
  saved in that state from this phase on (section 2), so in a release build a null Currency means no
  Loans; only the debug seeder can break that.
- **`LoanDao` has the reads and single-row inserts**, each `ABORT` on conflict, and a `RESTRICT` from
  Loan to Counterparty. It has no transaction that writes a Counterparty and its first Loan together,
  and nothing that writes the Currency. Both are added here.
- **Home and the Counterparty screen already draw a Direction** the way phase 0 settled it: never
  colour alone; the word, the arrow and the sign always appear, Lent filled in `primary`, Borrowed
  outlined in `tertiary` (`docs/phase-0.md`, *The palette*). Add Loan's Direction control follows
  the same cues.
- **The keyboard is an inset the shell already handles**: `Navigation.kt` pads `NavDisplay` with
  `consumeWindowInsets(insets).imePadding()`, and `MainActivity` sets `SOFT_INPUT_ADJUST_NOTHING`
  from API 30, keeping `adjustResize` below it. Add Loan is the first form in the app, so this is the
  first phase that reads that path on a device rather than trusting the comment (R6).
- **The bottom bar hides on a detail screen**, so a tab tap cannot discard a half-typed form.
- **The tip ask becomes reachable.** `tipAskDue` waits for ten Loans and thirty days since install;
  until now only the seeder could make ten Loans. Nothing changes in `tip/`. The debug build shows no
  card, because Play does not know `.debug`, so the first real reading of the ask is on an internal
  testing install, outside this phase.
- **`SettingsViewModel` reads only `app.preferences`**, with a comment saying Settings is reachable
  while the schema gate is open. It is not: `MainActivity` composes `SchemaMismatchScreen` *instead
  of* `MainNavigation` while a mismatch stands, and Settings is only an entry inside
  `MainNavigation`, where Home has already forced the container. The comment is a template leftover.
  Section 3 gives Settings the `CurrencyDao` and deletes the comment.
- **The debug seeder already holds a refusal.** `SampleData` stores Ola's second Loan as 1 250 minor
  units, 12,50 zł, so a change from złoty to yen is refused with exactly that Loan named, and every
  other seeded row divides by 100. The readings use it as it is.

## What is in, and what is deliberately not

**In:** Add Loan, reached from Home and from a Counterparty's screen: who (a typed name, matched to an
existing Counterparty, or a new Person or Pot), Direction, Amount parsed per ADR-0008, the Day,
defaulting to today. Saving writes the new Counterparty, if any, and the Loan in one transaction.
With no Currency set, Add Loan sends the owner to Settings first. Home's empty state says how to add
the first Loan; a Pot shows its icon on Home and on its own screen. Settings gains the Currency:
shown, chosen when unset, changed with a warning, rescaled exactly in one transaction, refused with
the blocking Loans listed when a fraction would be lost. Two pure functions with their tests (the
parser and the rescale plan) and two smaller ones (name matching, the date picker's day), the
instrumented DAO tests for both new transactions, Polish complete for every new string, and the
readings below.

**Not in, and each with a reason rather than a phase:**

- **The Due date field.** The column exists, but a Due date is only worth typing if it reminds, and
  the Reminder and its permission ask are phase 3's (`PLAN.md` rule 4: no ask before the feature that
  needs it, and a Due date with no Reminder behind it would be a promise the app does not keep).
- **Repayments, Settle, editing and deleting a Loan.** Phase 2, and the door opens after it. This
  phase writes Loans and changes the Currency; it does not change a Loan once written.
- **A total across every Counterparty on Home.** Not in `PLAN.md`, and summing Lent against Borrowed
  across different people answers no question an owner asks; each Balance is between two parties.
- **Reading the phone's contacts to fill *Who?*.** Refused by ADR-0009.
- **Exchange rates or a second Currency.** Refused by ADR-0008; the rescale re-labels, it never
  converts.
- **A Currency picker built from the app's own list.** The platform names currencies and their
  fraction digits (`java.util.Currency`, ICU underneath), localised in every language for free.
  Which of them the list offers is *Open for the owner*, item 4.

**The tempting one: a note on a Loan** (*"concert tickets"*). It looks like one more text field. It
is not: schema 1 has no column for it, and phases 1–3 add none, because the schema freezes at Phase
P's first upload. The honest cost is either a change to schema 1 *before* that upload or a schema 2
with its migration and the whole standing gate in `DOD.md`. That is a product decision with a
deadline, and it is the owner's: *Open for the owner*, item 6. Until it is answered, no session adds
a note field to Add Loan.

---

## Checkpoints

Each checkpoint leaves the app working and ships its copy complete in Polish
(`scripts/translation-gate.py` at merge). There is no gate checkpoint: everything sits on phase 0's
schema, and the one device unknown, which decimal key a phone's keyboard offers, cannot veto the
phase, because the parser accepts both marks.

| | Checkpoint | Merges | Depends on |
| --- | --- | --- | --- |
| **A** | **The two safety values**: `parseAmount` and `planRescale`, pure, with their tests; `matchCounterparty` and `dayFromPickerMillis` with theirs | with B, no commit type of its own | — |
| **B** | **The Currency in Settings**: shown, first choice, change with warning, exact rescale, refusal | `feat:` | A, the design consult |
| **C** | **Add Loan**: the screen, the one-transaction save, the entry points, Home's empty state, the Pot icon | `feat:` | A, B, the design consult |
| **D** | **Readings and documents**: edge-to-edge scenes, the readings table, ADR-0008's amendment, `PLAN.md` and the translator brief | `docs:` (scenes as `test:`) | C |

**A first, and it does not wait for the design consult.** The parser and the rescale plan are where
this phase can lose money silently, they have no UI, and ADR-0008 already specifies every edge they
are tested against. Writing them first means B and C are built on tested functions rather than
testing them afterwards.

**B before C**, against the order `PLAN.md`'s sentence reads in. Add Loan's first behaviour on a phone
whose region named no currency is to send the owner to Settings' Currency (ADR-0008), so that surface
has to exist first. B is also fully exercisable without C: the debug seeder writes the rows a rescale
needs, refusal included.

**B and C are two checkpoints, and may be two PRs** (`PLAN.md` rule 2). B is one transaction and a
few dialogs whose risk is arithmetic; C is the app's first form, whose risk is the keyboard, process
death and the name matching. Bundled, a stuck half hides which half is stuck.

---

## 1. Parsing a typed Amount

The rule is ADR-0008's, in full, and is not restated here. What this section adds is where it lives,
its shape, and the two bounds the ADR left open.

`ui/common/Amounts.kt` gains `parseAmount`, beside `formatAmount`, so the pair that reads and writes
the owner's numbers is one file:

```kotlin
sealed interface ParsedAmount {
    data class Valid(val minorUnits: Long) : ParsedAmount
    data object Empty : ParsedAmount
    data object Unreadable : ParsedAmount
    data object Ambiguous : ParsedAmount
    data class TooManyDecimals(val allowed: Int) : ParsedAmount
    data object Zero : ParsedAmount
    data object TooLarge : ParsedAmount
}

fun parseAmount(typed: String, fractionDigits: Int): ParsedAmount
```

It takes the Currency's `defaultFractionDigits`, never a locale: ADR-0008's rule is independent of the
app's language and the phone's. Each refusal is its own case so the field can say which mistake it
was (section 4's copy), and none of them saves anything.

**Two bounds the ADR does not state, decided here:**

1. **Zero is refused.** Rule 1 accepts `0`, and a Loan of nothing would be born Settled. A Loan
   records money that changed hands; nothing did.
2. **The whole part is below 10¹².** Stated in the Currency's major units, so it is invariant under
   a rescale, which preserves every value at the new scale. With the largest scale the picker can
   offer (three digits, or four if item 4 of *Open for the owner* keeps CLF and UYW), the largest
   stored value is below 10¹⁶, so a rescale's multiplication never overflows a `Long`, and SQLite's
   `SUM` over the Balances has room for some thousands of such Loans. Nobody lends a trillion złoty;
   the bound is there so that `amount * 1000` cannot wrap to a negative Amount, which is the one
   failure in this phase that would show a lender as a borrower.

**A third clarification, for currencies with more than three fraction digits.** ADR-0008's rule 2
calls a lone mark followed by exactly three digits ambiguous "when it has three". For a Currency with
four (CLF, UYW, if the picker offers them), `1,000` is just as ambiguous, so the rule reads "three or
more". The amendment records it.

## 2. Add Loan

### The route and the ViewModel

`NavigationKeys.kt` gains `AddLoan(counterpartyId: String? = null)`: `null` from Home, the
Counterparty's id from its screen, which then opens Add Loan with *Who?* already filled. One
`AddLoanViewModel`, one immutable `AddLoanUiState` (CLAUDE.md), under `ui/loan/`.

**The typed fields live in `SavedStateHandle`**, not in `remember`, and not only in the ViewModel: Add
Loan is the first screen where process death costs the owner something they typed. The Day is saved
as its ISO text, the Direction and the new Counterparty's kind by name.

### Who

- A text field. The suggestions are the existing Counterparties, from `observeBalances`, the query
  Home already uses; no new DAO query.
- **Matching** is `matchCounterparty(typed, existing)`, pure: trim, collapse inner runs of white
  space, then compare ignoring case with Kotlin's Unicode-aware `equals(ignoreCase = true)`. It is
  done in Kotlin, not SQL, because SQLite's `NOCASE` folds ASCII only, and `Łukasz` and `łukasz`
  would be two Counterparties and two Balances, the split ADR-0009 exists to prevent.
- A match is that Counterparty: its kind shows and cannot be changed here. No match is a new
  Counterparty, and the owner chooses Person or Pot, Person preselected, since most Loans are with
  somebody. Whether a match can ever be overridden into a second Counterparty of the same name is
  *Open for the owner*, item 1; this file assumes not.
- The stored name is the trimmed, collapsed text as typed. Empty after trimming is refused.

### Direction

Lent or Borrowed, the CONTEXT words, with the arrow and the filled-against-outlined cue from phase 0.
Whether one is preselected is *Open for the owner*, item 2; this file assumes neither is and Save waits
for a choice. For a Pot, the helper sentence under the choice differs (ADR-0009: "taken from the joint
savings" rather than "borrowed from Ola"); the Direction words do not.

### Amount

Parsed by `parseAmount` against the Currency read when the screen opened, on every change, with the
refusal shown on the field (section 4's copy). Typed text is kept as typed: the field never
reformats under the owner's fingers. The keyboard is `KeyboardType.Decimal`; some keyboards offer
only `.` and some only `,` on that layout, and the parser takes either.

### Day

A calendar day, defaulting to today in the phone's zone at the moment the screen opens. **No future
day**: CONTEXT defines the Day as the date a Loan *was* made. Material 3's `DatePicker` reports the
chosen day as **UTC midnight** in epoch millis; `dayFromPickerMillis` converts it with
`ZoneOffset.UTC`, never the system zone, because the system zone moves every pick one day back west
of Greenwich. Shown through `formatDay` in the in-app language.

### No Currency yet

With the Currency row null, Add Loan does not show a form that cannot be parsed: it says a Currency is
needed first and offers the way to Settings' Currency. After choosing one, the owner comes back to
Add Loan. How that reads is design placeholder U5.

### Saving

One new `LoanDao` transaction:

```kotlin
@Transaction
open suspend fun addLoan(
    newCounterparty: CounterpartyEntity?,
    loan: LoanEntity,
    parsedAgainst: String,
)
```

It re-reads the Currency code inside the transaction and refuses when it differs from
`parsedAgainst`, so an Amount parsed at two digits can never land under a Currency with three. It then
inserts the Counterparty, if new, and the Loan. Either both rows exist or neither does; a failure
leaves the form as it was, with the field values intact, and says the Loan was not saved.

Save is enabled only when every field is valid. A second tap while the write runs does nothing: the
state carries `saving`, and the ViewModel ignores Save while it is set, so one tap is one Loan.

After Save the screen closes; where it lands is design placeholder U4.

### Home and the Counterparty screen

- `home_empty`'s comment says *"nothing on this screen adds a loan yet"*; the copy is rewritten to say
  how to add one (`EmptyState`'s own rule: say what to do). Its Polish goes stale at the same time,
  which the gate will hold.
- The Pot's icon beside its name on Home and in its screen's title, from ADR-0009. Which icon is
  design placeholder U2.

## 3. The Currency in Settings

### What Settings shows

`SettingsViewModel` takes the `CurrencyDao` beside `app.preferences`, and the stale comment saying it
must not touch the container goes (section 0). The row shows the Currency's localised name and
code, through `java.util.Currency.getDisplayName(appLocale)`, so no string resource holds a currency
name. When the row is null, it says none is set, and that a Loan cannot be added until one is.

### The rescale plan

`planRescale` is pure, in `data/`:

```kotlin
sealed interface RescalePlan {
    data object Relabel : RescalePlan
    data class Multiply(val factor: Long) : RescalePlan
    data class Divide(val factor: Long) : RescalePlan
    data class Refused(val loanIds: List<String>) : RescalePlan
}

fun planRescale(
    oldDigits: Int,
    newDigits: Int,
    values: List<StoredValue>,
): RescalePlan
```

`StoredValue` is a Loan id with one minor-unit value, an Amount or a Repayment, so a Repayment with a
fraction names its Loan. Same digits is a re-label; more digits multiplies by 10^(new − old); fewer
divides by 10^(old − new) when every value is divisible, and is otherwise refused with the distinct
Loans in the way, in the order the Counterparty screen shows them. From a null Currency there is
nothing to rescale (section 2 guarantees no Loan without a Currency), so the choice is a plain write.

### The rescale itself

One `LoanDao` transaction, `changeCurrency(code, plan)`: for a divide it first re-runs the blocking
query inside the transaction (`amount % :factor != 0` over Loans and Repayments) and aborts if anything
appeared since the plan was made, then updates `loans.amount`, `repayments.amount` and the Currency
row together. `Math.multiplyExact` is not available in SQL, so the bound in section 1 is what keeps
`amount * :factor` inside a `Long`; the DAO test asserts it on the largest value the parser can
produce. Outstanding is never stored, so it follows from the rows (`OUTSTANDING_PER_LOAN`), and the
test reads it back rather than trusting that.

The Currency row is written only by this transaction and by `CurrencyRowOnOpen`'s `INSERT OR IGNORE`,
which never overwrites. *Restore default settings* does not touch it: it is data, not a preference
(ADR-0008), and `AppPreferences.Keys` is unchanged.

### The warning and the refusal

- **Before any allowed change with Loans stored**, the warning: the numbers are re-labelled, never
  converted at an exchange rate, with one of the owner's own Amounts shown before and after. Whether
  it also appears when no Loan exists is *Open for the owner*, item 3.
- **A refused change** names why (an Amount with a fraction the new Currency cannot hold) and lists
  the Loans in the way: Counterparty, Day, Amount. Nothing is written. In this phase the owner can only
  keep the Currency (*Known gaps*).

How both look is design placeholders U6 to U8.

---

## Design placeholders — answered by the design consult, not here

These are visual and layout decisions. A separate design consult is answering them; when it does,
each answer replaces its question here, and the copy in section 4 is revised to fit before it is
translated.

- **U1. Add Loan's layout and controls.** Where it is opened from (a floating action button on Home,
  an action on a Counterparty's screen, or both), the order of *Who?*, Direction, Amount and Day, the
  control for each (*Who?* as a text field with suggestions below it or as an exposed dropdown;
  Direction as a segmented button, two chips or two radio rows; Amount with the currency symbol as a
  prefix or suffix; the Day as a read-only field opening the date picker), and where Save sits (top
  bar action, or a full-width button kept above the keyboard).
- **U2. Person or Pot.** How a new name is marked as a Person or a Pot on Add Loan, how an existing
  Counterparty's kind is shown once matched, and the Pot's icon on Home, on its screen and in the
  suggestions.
- **U3. Direction for a Pot.** How the helper sentence under the Direction choice reads and sits for a
  Pot against a Person.
- **U4. After Save, and leaving unsaved.** Where Save lands (back where Add Loan was opened, or on the
  Counterparty's screen with the new Loan visible), and whether Back with typed content asks before
  discarding it.
- **U5. No Currency yet.** How Add Loan says a Currency is needed and hands over to Settings, and how
  the owner comes back.
- **U6. The Currency row and its picker.** Which Settings section the row sits in, how it shows the
  Currency (name, code, symbol) or *not set*, and whether choosing opens a full-screen searchable list
  or a dialog.
- **U7. The warning before a change.** A dialog or an inline confirmation, its example Amount before
  and after, and the emphasis of the confirm button.
- **U8. The refusal.** How the blocking Loans are listed (in the dialog, or on a screen of their own),
  and whether a listed Loan opens its Counterparty.
- **U9. The Amount's error.** Shown under the field as the owner types, or only when Save is tapped.

## Open for the owner

Each changes what users see or what data is kept, and none is settled by an ADR. A recommendation,
then the alternatives.

1. **Two people with the same name.** *Recommended:* a typed name that matches an existing
   Counterparty, ignoring case and surrounding spaces, always is that Counterparty; the owner tells two
   Olas apart by typing "Ola K.". It is ADR-0009's reason for offering existing names, applied without
   an escape hatch. *Alternatives:* a "this is someone else" choice on a match, creating a second
   Counterparty with the same name and two Balances that look alike on Home; or matching only on exact
   case, which lets `ola` and `Ola` split silently.
2. **Is a Direction preselected?** *Recommended:* neither; Save waits until Lent or Borrowed is
   chosen, because a Loan saved the wrong way round shows a lender as a borrower and, until phase 2,
   cannot be edited or deleted. *Alternatives:* Lent preselected, as the common case and one tap
   fewer; or the last Direction used, remembered as a preference.
3. **The warning before a Currency change when no Loan exists.** *Recommended:* skip it; with nothing
   stored there is nothing to re-label, and the warning would describe no number. *Alternative:*
   always warn, the literal reading of ADR-0008's *"Settings warns before the change"*.
4. **Which currencies the picker offers.** *Recommended:* the ISO 4217 currencies in current use
   (ICU's `android.icu.util.Currency.isAvailable` for today, available from API 24), with a fraction
   digit count of 0 to 3, plus whatever the row already holds; read the list's length on a device
   before deciding. *Alternatives:* everything `java.util.Currency.getAvailableCurrencies()` returns,
   which includes historical currencies and the four-digit units of account CLF and UYW; or a short
   list of common currencies with a search for the rest.
5. **Renaming, re-kinding and deleting a Counterparty.** ADR-0009 promises renaming and no phase owns
   it; with no delete either, a mistyped new name stays on Home until one does. *Recommended:* phase
   2, beside editing and deleting a Loan, before the door: rename, Person-or-Pot, and delete of a
   Counterparty with no Loans (`RESTRICT` already refuses the rest). *Alternatives:* in this phase, as
   checkpoint C's second half; or after the door, accepting that closed testers meet the typo.
6. **A note on a Loan.** *Recommended:* decide before Phase P's first upload, because after it the
   answer costs a migration on every install. If yes, a nullable `note` column, added while the only
   installs are development ones, and shown on Add Loan in this phase. *Alternatives:* no note, the
   current plan, keeping Add Loan to four fields; or a note later, as schema 2 with `MIGRATION_1_2`
   and the full standing gate.

---

## 4. Strings and translation

Every string below is new, under the existing comment style in `values/strings.xml`, and is
**draft English**: the design consult (U1 to U9) and review settle the wording first, and Polish is
written once, against the settled English, before the merge (`scripts/translation-gate.py
origin/main`; CLAUDE.md's rule). `python3 scripts/translation-gate.py --report` lists what is owed at
any point.

| Resource | Draft English | Note for the translator |
| --- | --- | --- |
| `home_empty` (rewritten) | No loans yet. Add one to see who owes whom here. | goes stale in Polish; re-translate |
| `home_add_loan` | Add a loan | the entry point's label or content description |
| `add_loan_title` | Add a loan | |
| `add_loan_who` | Who? | the Counterparty field; CONTEXT's UI word |
| `add_loan_kind_person` | A person | |
| `add_loan_kind_pot` | Shared money (a pot) | the Pot; joint savings, a family fund |
| `add_loan_direction_lent_hint` | Owed to you | may reuse `home_balance_owed_to_you` instead |
| `add_loan_direction_borrowed_hint` | You owe | may reuse `home_balance_you_owe` instead |
| `add_loan_direction_borrowed_pot_hint` | Taken out of shared money | the Pot's sentence (ADR-0009) |
| `add_loan_amount` | Amount | |
| `add_loan_amount_unreadable` | Type a number, such as %1$s | `%1$s` is an example Amount formatted for the Currency |
| `add_loan_amount_ambiguous` | %1$s could mean two amounts. Write it as %2$s or %3$s. | both readings, formatted |
| `add_loan_amount_decimals` (plurals) | At most %1$d decimal place / places | quantity = the Currency's digits; Polish one, few, many, other |
| `add_loan_amount_whole` | Whole numbers only in this currency | for zero-digit currencies such as the yen |
| `add_loan_amount_zero` | The amount has to be more than zero | |
| `add_loan_amount_too_large` | That amount is too large | |
| `add_loan_day` | Day | the Day the Loan was made |
| `add_loan_save` | Save | |
| `add_loan_failed` | The loan was not saved. Try again. | |
| `add_loan_no_currency` | Choose a currency first. Every amount in the app is in it. | |
| `add_loan_choose_currency` | Choose a currency | the hand-off to Settings |
| `settings_currency` | Currency | |
| `settings_currency_not_set` | Not set. A loan cannot be added until it is. | |
| `settings_currency_search` | Search currencies | if U6 picks a searchable list |
| `settings_currency_change_title` | Change the currency to %1$s? | `%1$s` the localised currency name |
| `settings_currency_change_body` | Every amount keeps its number and is shown in %1$s. Nothing is converted at an exchange rate: %2$s becomes %3$s. | the owner's own Amount before and after |
| `settings_currency_change_confirm` | Change | |
| `settings_currency_refused` (plurals) | %1$d loan has / %1$d loans have an amount %2$s cannot hold, because it has fewer decimal places. | quantity = the count; Polish one, few, many, other |

**What needs no resource:** currency names and symbols (the platform's, localised), every formatted
Amount and Day (`formatAmount`, `formatDay`), and the Counterparty's name, which the owner typed.

**Traps for Polish, named before the draft so the English can avoid them** (`docs/translator-brief.md`
§6):

- **The owner's gender** (§6.2). "Pożyczyłeś" and "pożyczyłaś" differ; the English above uses noun
  phrases and the existing Direction words, which the Polish already renders as *Udzielona* and
  *Zaciągnięta*, agreeing with *pożyczka* rather than with anybody.
- **A typed name cannot be inflected** (§6.3). No string puts a Counterparty's name after a
  preposition: *"borrowed from %1$s"* would need the genitive of whatever the owner typed. The names
  sit beside sentences, never inside them.
- **Plurals** (§6.4): the decimal-places error and the refusal count are `<plurals>`, never
  `if (n == 1)`.

**The translator brief's §5 vocabulary table** is still the template's empty one. This phase is where
the words first repeat, so it gets its rows here, against the Polish already shipped: Loan
(*pożyczka*), Lent (*Udzielona*), Borrowed (*Zaciągnięta*), Settled (*Rozliczona*), Outstanding (*Do
spłaty*), Amount, Day, Currency, *Who?*, Person and Pot, the last six chosen once and then kept.

---

## Tests

`PLAN.md` rule 3: a pure function that computes or bounds a safety value gets its test in the phase
that introduces it. Two are named by ADR-0008; two more are small and fail in the same direction.

| Test | Where | What it stands in front of, and the direction it fails in |
| --- | --- | --- |
| `AmountParserTest` | `app/src/test/…/ui/common/` | Every edge ADR-0008 lists, in złoty, yen and dinars; `,` `.`, a space, a no-break and a narrow no-break space as grouping; trimming; zero; the 10¹² bound; three-digit ambiguity at three and four digits. Fails as 100,50 zł stored as 10 050 zł. |
| `RescalePlanTest` | `app/src/test/…/data/` | Relabel, multiply and divide factors in both directions; a refusal names each blocking Loan once, including one blocked only by a Repayment. Fails as 100,50 zł becoming ¥10 050. |
| `CounterpartyMatchTest` | `app/src/test/…/ui/loan/` | Trim, inner white space, Unicode case (`Łukasz`/`łukasz`, `Ola`/`OLA`), no match across diacritics (`Lukasz`/`Łukasz`). Fails as one person split into two Balances. |
| `PickerDayTest` | `app/src/test/…/ui/loan/` | The picker's UTC-midnight millis map to the same calendar day under a default zone west and east of UTC. Fails as every Loan recorded a day early. |
| `LoanDaoTest` (extended) | `app/src/androidTest/…/data/` | `addLoan` writes Counterparty and Loan together, writes only the Loan for an existing Counterparty, and writes neither when the Currency changed since parsing. `changeCurrency`, per ADR-0008's *Consequences*: every Loan's Outstanding and Settled state unchanged across an allowed change in either direction, a lossy change refused with every row as it was, and the largest parseable Amount multiplied by 1000 without wrapping. |

No `SchemaGateTest` or migration test changes: `APP_SCHEMA_VERSION` does not move, and
`schema-gate.py` would fail the build if it did.

## Documents this phase amends

- **ADR-0008**, a dated amendment: zero is refused; the whole part is below 10¹² in major units, and
  why that bounds the rescale; rule 2's ambiguity applies at three *or more* fraction digits; which
  currencies the picker offers, once item 4 is answered.
- **ADR-0009**: none, unless item 1 is answered against the recommendation, in which case its
  *Decision* paragraph changes and a new ADR supersedes it.
- **`PLAN.md`**: the phase ticked; *The values a test stands in front of* reconciled to four for
  phase 1; and item 5's answer placed in phase 2's paragraph if it lands there.
- **`CONTEXT.md`**: nothing, unless item 1 changes how a name maps to a Counterparty, or the design
  consult gives the Pot a different UI word.
- **`docs/translator-brief.md`**: the §5 vocabulary rows above.
- **`CLAUDE.md`**: `ui/loan/` in *Layout*, if a reviewer finds `ui/` alone too thin a hint.
- **`scripts/edge-to-edge.py`**: SCENES gain `add-loan` (from Home), `add-loan-counterparty` (from
  Ola's screen) and `settings-currency`; `screenshots.py` imports them unchanged.
- **The privacy policy, `docs/play-app-content.md` and the store listing**: nothing. No permission, no
  network, nothing collected or shared; a typed name and an Amount stay in the database that ADR-0005
  already describes.

## Known gaps, deliberately

- **A refused Currency change has no way through in this phase.** ADR-0008 sends the owner to edit the
  blocking Loans; editing is phase 2. Accepted because phase 1 reaches no stranger (the door is after
  phase 2) and the refusal loses nothing. Closing it here would mean building phase 2's edit early.
- **A Loan saved wrong stays wrong until phase 2**, for the same reason. It is why item 2 recommends no
  preselected Direction.
- **The debug seeder can write Loans under a null Currency.** `formatAmount` then shows bare minor
  units. Debug-only, and it is the seeder's job to write rows the UI cannot; a release build has no
  path to it.
- **The tip ask's first real showing is not read in this phase.** The debug build cannot show it
  (Play does not know `.debug`), and the internal-track install that can is Phase P's.

---

## Readings

`PLAN.md` rule 3: filled in as the phase runs, from the device, with the command that read it.
On the emulators (`pixel-api37`, `urgent-api26`, `small-api30`, `tablet-api36`), per the owner's
standing rule; nothing here needs a vendor ROM.

**A database pulled without its `-wal` reads like a pass and is nothing** (CLAUDE.md, *Environment
notes*): the newest writes sit in the WAL. Every DB reading pulls `ioutracker.db`, `-wal` and `-shm`
together and runs `PRAGMA wal_checkpoint(TRUNCATE)` on the host before reading.

Add Loan is the first screen whose route key carries an argument written by this app's own code,
and `@Serializable` keys are what R8 breaks without a keep rule, so R2 runs on
`-PreleaseShapedDebug` (minified, `BuildConfig.DEBUG` false) and starts from a wipe, so the only rows
are ones Add Loan wrote.

```bash
adb exec-out run-as com.maksforge.ioutracker.debug sh -c 'cd databases && tar c ioutracker.db ioutracker.db-wal ioutracker.db-shm' | tar x -C "$OUT"
```

| # | Reading | Command | Result |
| --- | --- | --- | --- |
| R1 | Under app language English, Currency PLN, `100.50`, `100,5`, `1,000` and `1 000` save as 10050, 10050, 100000 and 100000 minor units and show as `PLN 100.50` and `PLN 1,000.00` | Add Loan by hand; the pull above, then `sqlite3 ioutracker.db 'SELECT amount FROM loans ORDER BY rowid'` | *(to read)* |
| R2 | The same four under Polish, on a release-shaped build, show as `100,50 zł` and `1000,00 zł` (or `1 000,00 zł`, as the platform groups) and store the same numbers as R1 | `./gradlew installDebug -PreleaseShapedDebug`, app language Polish, then as R1 | *(to read)* |
| R3 | A typed `ola` while `Ola` exists adds no Counterparty; a new name adds exactly one, of the kind chosen | `sqlite3 ioutracker.db 'SELECT name, kind FROM counterparties'` before and after | *(to read)* |
| R4 | Seeded data, PLN to JPY: refused, naming only Ola's 12,50 zł Loan, every row unchanged | seed, attempt the change, pull, `SELECT amount FROM loans; SELECT amount FROM repayments` against the seed | *(to read)* |
| R5 | A wiped install, Currency JPY, a Loan of 1500, then to PLN (rows ×100, `1500,00 zł`), then back to JPY (allowed, 1500 again) | the pull and `SELECT` after each change | *(to read)* |
| R6 | Add Loan with the keyboard up keeps Save and the focused field on screen, on API 26 (`adjustResize`) and API 30+ (`imePadding`) | `adb shell input tap` into Amount, `adb exec-out screencap -p > add-loan-ime-<serial>.png` | *(to read)* |
| R7 | A half-filled Add Loan survives process death | fill, Home button, `adb shell am kill com.maksforge.ioutracker.debug`, reopen from recents, screenshot | *(to read)* |
| R8 | A system region that names no currency leaves the row null, and Add Loan hands over to Settings instead of showing the form | on an emulator: `adb root`, `adb shell setprop persist.sys.locale en-AQ`, `adb shell stop && adb shell start`, a fresh install, then Add Loan | *(to read; if `en-AQ` maps to a currency, the region is chosen from `CurrencyRegionTest`'s null cases instead)* |
| R9 | Edge-to-edge is clean on the new scenes in every cell | `python3 scripts/edge-to-edge.py --out DIR --assert-clean --retry-unreached 2 --scene add-loan,add-loan-counterparty,settings-currency` | *(to read)* |
| R10 | TalkBack reads each Add Loan field's label, the Direction as a choice with its state, and the Amount's error when it appears | TalkBack on, swipe through Add Loan on `pixel-api37` | *(to read)* |
| R11 | The picker's list, as item 4 is answered: how many currencies, and whether historical ones appear | Settings → Currency on `pixel-api37` and `urgent-api26` | *(to read)* |
