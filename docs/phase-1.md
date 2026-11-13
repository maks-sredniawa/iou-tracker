# Phase 1 — A Loan can be recorded, Lent or Borrowed, with a Person or a Pot, in the owner's Currency

Sequence lives in [`PLAN.md`](PLAN.md); the live worklist lives in [`DOD.md`](DOD.md). This phase
builds on ADR-0008 (one Currency per install, Amounts in minor units: the parser, the rescale, the
refusal) and ADR-0009 (Counterparties typed by hand, a Pot is one of them), and amends ADR-0008 with
two bounds it left open (*Documents this phase amends*). It touches no schema, so ADR-0001 is not
reopened. [`CONTEXT.md`](../CONTEXT.md) is owed nothing: item 1 under *Open for the owner* was
settled as ADR-0009 already maps a name to a Counterparty, and the design consult kept the Pot's word
(its UI copy says *shared money*, which is a gloss, not a second word).

**What closing this phase means.** Today the app opens on Home, which lists Counterparties with their
Balance, and the only way a Loan gets into the database is the debug seeder. After this phase the
owner adds a Loan from the app: types who, picks Lent or Borrowed, types the Amount in either decimal
style, picks the Day, and saves. A new name becomes a Counterparty (a Person or a Pot) in the same
write; a name already used is that Counterparty. Settings shows the Currency and changes it, rescaling
every stored Amount and Repayment exactly, warning first and refusing a change that would lose a
fraction. The difference a device can be asked about: on a minified, release-shaped build started
from a wipe, so every row is one Add Loan wrote, a Loan typed as `100,50` under Polish and as `100.50`
under English both read back as 10050 minor units in the pulled database and as the same Amount on
screen, and a change from złoty to yen is refused while a 12,50 zł Loan exists.

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
  phase in `PLAN.md` owned renaming, changing a kind, or deleting a Counterparty. Settled for phase 2
  (*Open for the owner*, item 5), and `PLAN.md`'s phase 2 paragraph now says so.
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
  Which of them the list offers is settled under *Open for the owner*, item 4.

**The tempting one: a note on a Loan** (*"concert tickets"*). It looks like one more text field. It
is not: schema 1 has no column for it, and phases 1–3 add none, because the schema freezes at Phase
P's first upload. The honest cost is either a change to schema 1 *before* that upload or a schema 2
with its migration and the whole standing gate in `DOD.md`. The owner settled it (*Open for the
owner*, item 6): a Loan has no note, and schema 1 stays as it is.

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

**B and C are two checkpoints, and may be two PRs.** This borrows `PLAN.md` rule 2's reasoning, which
splits *phases*, inside one phase: the two halves differ in kind more than in cost. B is one
transaction and a few dialogs whose risk is arithmetic; C is the app's first form, whose risk is the
keyboard, process death and the name matching. Bundled, a stuck half hides which half is stuck.

---

## 1. Parsing a typed Amount

ADR-0008's rule is the base and is not restated here. What this section adds is where it lives, its
shape, the two bounds the ADR left open, and the refinements the owner settled while the parser was
written. None of them weakens the ADR's safety point: a separator the parser could misread is
refused, never guessed.

`ui/common/Amounts.kt` gains `parseAmount`, beside `formatAmount`, so the pair that reads and writes
the owner's numbers is one file:

```kotlin
sealed interface ParsedAmount {
    data class Valid(val minorUnits: Long) : ParsedAmount
    data object Empty : ParsedAmount
    data object Unreadable : ParsedAmount
    data class Ambiguous(val asDecimal: Long, val asGrouped: Long) : ParsedAmount
    data class TooManyDecimals(val allowed: Int) : ParsedAmount
    data object Zero : ParsedAmount
    data object TooLarge : ParsedAmount
}

fun parseAmount(typed: String, fractionDigits: Int): ParsedAmount
```

It takes the Currency's `defaultFractionDigits`, never a locale: ADR-0008's rule is independent of the
app's language and the phone's. Each refusal is its own case so the field can say which mistake it
was (section 4's copy), and none of them saves anything. `Ambiguous` carries both readings in minor
units, so the test pins what each would have saved.

**Two bounds the ADR does not state, decided here:**

1. **Zero is refused.** Rule 1 accepts `0`, and a Loan of nothing would be born Settled. A Loan
   records money that changed hands; nothing did.
2. **The whole part is below 10¹².** Stated in the Currency's major units, so it is invariant under
   a rescale, which preserves every value at the new scale. With the largest scale the picker can
   offer (three digits, since item 4 of *Open for the owner* leaves out CLF and UYW), the largest
   stored value is below 10¹⁵, so a rescale's multiplication never overflows a `Long`, and SQLite's
   `SUM` over the Balances has room for about 9 000 Loans at the bound (`Long.MAX_VALUE` is about
   9.2 × 10¹⁸). Nobody lends a trillion złoty; the bound is there so that `amount * 1000` cannot wrap
   to a negative Amount, which is the one failure in this phase that would show a lender as a
   borrower.

**Refinements to rules 2 and 3, settled by the owner:**

1. **A lone mark before exactly three digits is grouping only when rule 4 allows it**, which means
   one to three digits before the mark. Otherwise it is the decimal separator, in every Currency:
   `1234,500` złoty is 1 234,50 zł, `1234,567` złoty is TooManyDecimals, and `1234,000` dinars is
   1 234 dinars. When both readings are valid it is grouping below three fraction digits, and
   Ambiguous at three *or more*. ADR-0008 says "when it has three", but for a Currency with four
   (CLF, UYW, if the picker ever offers them) `1,000` is just as ambiguous. So `1,234` dinars is
   Ambiguous.
2. **A whole part of zero makes the mark the decimal separator, in every Currency.** Nobody groups a
   number that starts at `0`: `0,001` dinars is one fils, `0,500` złoty is 0,50 zł, and `0,505`
   złoty is TooManyDecimals.
3. **Trailing zeros past the Currency's fraction digits are accepted when every extra digit is
   `0`**, in every Currency: `1,0000` złoty is 1 zł, `100,0` yen is ¥100, and `100,5` yen is
   refused. This is how the owner gets out of an ambiguity: `1,2340` is one dinar and 234 fils, and
   `1 234`, with a space, is 1 234 dinars. The ambiguity copy names those two forms, built from what
   was typed. It does not name a formatted Amount, which could be ambiguous again.
4. **A decimal separator needs a whole part.** `,50` and `.5` are Unreadable.

## 2. Add Loan

### The route and the ViewModel

`NavigationKeys.kt` gains `AddLoan(counterpartyId: String? = null)`: `null` from Home, the
Counterparty's id from its screen, which then opens Add Loan with *Who?* already filled. One
`AddLoanViewModel`, one immutable `AddLoanUiState` (CLAUDE.md), under `ui/loan/`. It takes the
`SavedStateHandle`, `LoanDao` and `CurrencyDao`.

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
- A match is that Counterparty: its kind shows and cannot be changed here, and there is no way to
  override it into a second Counterparty of the same name (*Open for the owner*, item 1). No match is
  a new Counterparty, and the owner chooses Person or Pot, Person preselected, since most Loans are
  with somebody.
- The stored name is the trimmed, collapsed text as typed. Empty after trimming is refused.

### Direction

Lent or Borrowed, the CONTEXT words, with the arrow and the filled-against-outlined cue from phase 0.
Neither is preselected (*Open for the owner*, item 2), so a Save with none chosen writes nothing and
shows the Direction's error (*Saving*). For a Pot, the helper
sentence under the choice differs (ADR-0009: "taken from the joint savings" rather than "borrowed from
Ola"); the Direction words do not.

### Amount

Parsed by `parseAmount` on every change of the text *or of the Currency*. The ViewModel collects
`CurrencyDao.observeCode()` rather than reading it once, because the Currency can arrive while the
screen is open (*No Currency yet*), and each parse remembers the code it ran against, which is what
Save passes as `parsedAgainst`. A valid parse is echoed under the field at once. A refusal shows only
when the field loses focus or on Save, never mid-typing (U9; section 4's copy). Typed text is kept as
typed: the field never reformats under the owner's fingers. The keyboard is `KeyboardType.Decimal`;
some keyboards offer only `.` and some only `,` on that layout, and the parser takes either.

### Day

A calendar day, defaulting to today in the phone's zone at the moment the screen opens. **No future
day**: CONTEXT defines the Day as the date a Loan *was* made. Material 3's `DatePicker` reports the
chosen day as **UTC midnight** in epoch millis; `dayFromPickerMillis` converts it with
`ZoneOffset.UTC`, never the system zone, because the system zone moves every pick one day back west
of Greenwich. Shown through `formatDay` in the in-app language.

### No Currency yet

With the Currency row null, Add Loan does not show a form that cannot be parsed: it says a Currency is
needed first and offers the way to the Currency picker (section 3). After choosing one, the owner
comes back to the same `AddLoanViewModel`, whose `observeCode()` collection now holds the code, so
the form appears with anything already typed intact. How that reads is *Design*, U5.

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

Save stays enabled (U9). A tap with any field empty or refused writes nothing, shows every field's
error at once and moves focus to the first. Before that, each field's error shows only when the field
loses focus. A second tap while the write runs does nothing: the state carries `saving`, and the
ViewModel ignores Save while it is set, so one tap is one Loan.

After Save the screen closes; where it lands is *Design*, U4.

### Home and the Counterparty screen

- `home_empty`'s comment says *"nothing on this screen adds a loan yet"*; the copy is rewritten to say
  how to add one (`EmptyState`'s own rule: say what to do). Its Polish goes stale at the same time,
  which the gate will hold.
- The Pot's icon beside its name on Home and in its screen's title, from ADR-0009. Which icon is
  *Design*, U2.

## 3. The Currency in Settings

### What Settings shows

`SettingsViewModel` takes the `CurrencyDao` beside `app.preferences`, only to show the row, and the
stale comment saying it must not touch the container goes (section 0). The row shows the Currency's
localised name and code, through `java.util.Currency.getDisplayName(appLocale)`, so no string
resource holds a currency name. When the row is null, it says none is set, and that a Loan cannot be
added until one is. A tap opens the picker; Settings itself never changes the Currency.

### The screens, their keys and their ViewModels

Two new full-screen destinations (U6, U8), each with its own key in `NavigationKeys.kt` and its own
ViewModel under `ui/currency/`, per the one-ViewModel-per-screen rule:

| Key | Screen | ViewModel | Takes |
| --- | --- | --- | --- |
| `CurrencyPicker` (`data object`) | The list, the warning dialog (U7), the change | `CurrencyPickerViewModel` | `CurrencyDao`, `LoanDao` |
| `CurrencyRefused(toCode: String)` | The reason and the blocking Loans (U8) | `CurrencyRefusalViewModel` | `toCode`, `CurrencyDao`, `LoanDao` |

The picker is pushed from Settings' row and from Add Loan's *No Currency yet*, and pops back to
whichever pushed it. A refused choice pushes `CurrencyRefused` over the picker, so Back returns to the
list.

**On a choice, `CurrencyPickerViewModel`:**

1. Reads the stored code, `fromCode`, from `observeCode()`'s latest value. A null `fromCode` has
   nothing to rescale: it goes straight to step 4 with `Relabel`.
2. Reads `LoanDao.storedValues()` and runs `planRescale`. `Refused` pushes `CurrencyRefused(toCode)`
   and writes nothing.
3. Reads `LoanDao.newestAmount()`. Null means no Loan is stored and skips the warning (item 3);
   otherwise the warning shows that Amount before and after.
4. On confirm, or straight away when there is no warning, calls `changeCurrency(fromCode, toCode,
   plan)`. The state carries `changing`, and a confirm while it is set does nothing.

**New `LoanDao` reads:**

- `storedValues(): List<StoredValue>`, suspend, one shot: every Loan's Amount and every Repayment's,
  each with its Loan id, ordered by its Loan's Day and then `rowid`, both descending, as
  `observeLoansOf` orders a Counterparty's Loans.
- `newestAmount(): Long?`, suspend: the Amount of the Loan that ordering puts first, for the warning's
  before-and-after.
- `observeBlockingLoans(factor: Long): Flow<List<BlockingLoan>>`: every Loan whose Amount, or any of
  whose Repayments, is not divisible by `factor`. Each comes with its Counterparty's id, name and kind,
  and its Day, Amount and Outstanding, enough for a `LoanRow` card and the tap to its Counterparty. It
  is the same predicate `changeCurrency` re-checks inside its transaction. `CurrencyRefusalViewModel`
  takes `factor` from the digits of the stored code and of `toCode`, and the plural's count is the
  list's size.

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
Loans in the way, each once, in the order of their first appearance in `values`. `planRescale`
sorts nothing; `storedValues()` supplies the order. From a null Currency there is nothing to rescale
(section 2 guarantees no Loan without a Currency), so the choice is a `Relabel`.

### The rescale itself

One `LoanDao` transaction:

```kotlin
@Transaction
open suspend fun changeCurrency(fromCode: String?, toCode: String, plan: RescalePlan): Boolean
```

It first re-reads the Currency code inside the transaction and writes nothing, returning `false`,
unless it is still `fromCode`; the comparison is SQL's `IS`, because `= NULL` is never true. A
multiply is not idempotent, and a divide whose values happen to divide again is not either. Without
this guard a second confirm tap, or a plan made against a Currency that has since changed, applies the
factor twice and turns 100,50 zł into 10 050 zł with no refusal. It is the guard `addLoan` gets from
`parsedAgainst`. On `false` the picker re-plans from the code now stored. `Refused` never reaches
the transaction. For a divide it then re-runs the blocking query (`amount % :factor != 0` over Loans
and Repayments) and aborts if anything appeared since the plan was made. Last, it updates
`loans.amount`, `repayments.amount` and the Currency row together. `Math.multiplyExact` is not
available in SQL, so the bound in section 1 is what keeps `amount * :factor` inside a `Long`; the DAO
test asserts it on the largest value the parser can produce. Outstanding is never stored, so it
follows from the rows (`OUTSTANDING_PER_LOAN`), and the test reads it back rather than trusting that.

The Currency row is written only by this transaction and by `CurrencyRowOnOpen`'s `INSERT OR IGNORE`,
which never overwrites. *Restore default settings* does not touch it: it is data, not a preference
(ADR-0008), and `AppPreferences.Keys` is unchanged.

### The warning and the refusal

- **Before any allowed change with Loans stored**, the warning: the numbers are re-labelled, never
  converted at an exchange rate, with one of the owner's own Amounts shown before and after. With no
  Loan stored there is no warning (*Open for the owner*, item 3).
- **A refused change** names why (an Amount with a fraction the new Currency cannot hold) and lists
  the Loans in the way: Counterparty, Day, Amount. Nothing is written. In this phase the owner can only
  keep the Currency (*Known gaps*).

How both look is *Design*, U6 to U8.

---

## Design — from the design consult

The copy in section 4 is revised to fit these before it is translated.

```
┌──────────────────────────────┐      ┌──────────────────────────┐
│ ←  Add Loan                  │      │ ←  Currency              │
│ [ ↗ Lent      | ↙ Borrowed ] │      │ [ Search               ] │
│ Who?                         │      │ ● Polish złoty      PLN  │
│ ┌──────────────────────────┐ │      │ ○ Euro              EUR  │
│ │ Ola                      │ │      │ ○ Japanese yen      JPY  │
│ └──────────────────────────┘ │      └──────────────────────────┘
│ New:  (• Person) ( Pot )     │
│ ┌────────────────────┬─────┐ │
│ │ 100,50             │ PLN │ │
│ └────────────────────┴─────┘ │
│ 100,50 zł                    │
│ Day            12 Nov 2026 › │
├──────────────────────────────┤
│ [          Save            ] │
└──────────────────────────────┘
```

- **U1. Add Loan's layout and controls.**
  - **Opened from:** an `ExtendedFloatingActionButton` "Add Loan" on Home and on a Counterparty's
    screen. It is also the action of both `EmptyState`s.
  - **Field order:** Direction, Who, Amount, Day. This reads as a sentence and matches
    `CounterpartyScreen`'s rows.
  - **Direction:** a `SingleChoiceSegmentedButtonRow` with `DirectionLabel`'s arrows and words. The
    selected tone is primary for Lent and tertiary for Borrowed, as in the rows.
  - **Who?:** an `OutlinedTextField` inside an `ExposedDropdownMenuBox`, offering existing
    Counterparties through the section 2 match.
  - **Amount:** an `OutlinedTextField` with the Currency *code* as its suffix.
  - **Day:** a row opening the date picker.
  - **Save:** a full-width filled `Button` in the Scaffold's `bottomBar`. It stays above the keyboard
    through the shell's `imePadding()` on `NavDisplay` (section 0), which consumes the inset, so the
    bar carries none of its own.
  - **Focus:** opened from Home, focus starts in Who? with the keyboard up. Opened from a
    Counterparty's screen, Who? is prefilled and focus starts in Amount.
- **U2. Person or Pot.**
  - **A new name:** two `FilterChip`s under Who?, *Person* (the default) and *Pot*. They show only
    while the name matches no Counterparty.
  - **An existing Counterparty:** shows its Person/Pot icon in the suggestions and on its rows. The
    chips are hidden.
- **U3. Direction for a Pot.** For a Pot only, one supporting line under the segmented row says what
  each Direction means. The wording is in section 4.
- **U4. After Save, and leaving unsaved.**
  - **Save** pops back to where Add Loan was opened. The new Balance shows there.
  - **Back** asks before discarding only once something has been typed.
- **U5. No Currency yet.** The form is replaced by an `EmptyState`-style message, "Choose a Currency
  before the first Loan", with one button. The button opens the Currency picker, and choosing one
  returns to the form.
- **U6. The Currency row and its picker.**
  - **The row:** a `LinkRow` (`LinkTarget.Inside`), first in Settings' Data section. Its subtitle is
    the Currency's name and code, or "Not chosen".
  - **The picker:** a full-screen destination with a search field and a `LazyColumn` of radio rows
    (localised name, code). The current Currency and the system region's Currency are pinned on top.
    About 150 entries rule out chips or a dialog.
- **U7. The warning before a change.**
  - **Form:** an `AlertDialog` shaped like `RestoreDefaultsRow`'s.
  - **The confirm button** is in the default colour, not `error`, because the rescale is exact and
    reversible.
  - **The body** shows one of the owner's own Amounts before and after the change.
- **U8. The refusal.**
  - **When:** checked before any warning.
  - **Where:** a full-screen destination with the reason and the blocking Loans as `LoanRow` cards.
  - **The list:** in this phase it is read-only. A tap opens the Loan's Counterparty, and the copy
    does not promise editing until phase 2 brings it.
- **U9. The Amount's error.**
  - **When it shows:** `isError` with supporting text, shown when the field loses focus or on Save,
    never mid-typing.
  - **The echo:** while the text parses, the supporting text echoes it through `formatAmount`. That
    echo is the defence against the grouping rule.
  - **Save** stays enabled. Tapping it with an error focuses the first one.

## Open for the owner

Each changes what users see or what data is kept, and none is settled by an ADR. All six are now
settled by the owner, each as recommended.

1. **Two people with the same name.** *Settled (2026-11-13):* a typed name that matches an existing
   Counterparty, ignoring case and surrounding spaces, always is that Counterparty, with no "someone
   else" escape; the owner tells two Olas apart by typing "Ola K.".
2. **Is a Direction preselected?** *Settled (2026-11-13):* neither; a Save before Lent or Borrowed is
   chosen writes nothing and shows the Direction's error.
3. **The warning before a Currency change when no Loan exists.** *Settled:* skipped. With nothing
   stored there is nothing to re-label, and the design consult reached the same answer on its own.
4. **Which currencies the picker offers.** *Settled:* the ISO 4217 currencies in current use (ICU's
   `android.icu.util.Currency.isAvailable` for today, available from API 24), with 0 to 3 fraction
   digits, plus whatever the row already holds. That leaves out historical currencies and the
   four-digit units of account, CLF and UYW. Building it showed ICU still lists the X codes that are
   not currencies and the fund codes as available, so those go too; ADR-0008's amendment has the
   list. *Settled (2026-11-13), after R11 on API 26:* the platform's answer is corrected by a short
   built-in list of withdrawn codes it may still offer and current ones it may lack (ADR-0008's
   amendment). Reading R11 records the list's length on a device.
5. **Renaming, re-kinding and deleting a Counterparty.** *Settled (2026-11-13):* phase 2, beside
   editing and deleting a Loan, before the door: rename, Person-or-Pot, and delete of a Counterparty
   with no Loans (`RESTRICT` already refuses the rest).
6. **A note on a Loan.** *Settled (2026-11-13):* no note. Add Loan keeps its four fields, and schema 1
   is unchanged, with no migration.

---

## 4. Strings and translation

Every string below is new, under the existing comment style in `values/strings.xml`, and is
**draft English**: *Design* (U1 to U9) and review settle the wording first, and Polish is
written once, against the settled English, before the merge (`scripts/translation-gate.py
origin/main`; CLAUDE.md's rule). `python3 scripts/translation-gate.py --report` lists what is owed at
any point.

| Resource | Draft English | Note for the translator |
| --- | --- | --- |
| `home_empty` (rewritten) | No loans yet. Add one to see who owes whom here. | goes stale in Polish; re-translate |
| `home_add_loan` | Add a loan | the entry point's label or content description |
| `add_loan_title` | Add a loan | |
| `add_loan_discard_title` | Discard this loan? | U4: Back after something was typed |
| `add_loan_discard_body` | What you typed will be lost. | |
| `add_loan_discard_confirm` | Discard | |
| `add_loan_discard_keep` | Keep editing | |
| `add_loan_who` | Who? | the Counterparty field; CONTEXT's UI word |
| `add_loan_who_empty` | Type a name | Save with *Who?* empty |
| `add_loan_kind_person` | A person | |
| `add_loan_kind_pot` | Shared money (a pot) | the Pot; joint savings, a family fund |
| `counterparty_pot_icon` | Shared money | content description of the Pot's icon on Home, in its screen's title and in the suggestions; read by TalkBack beside the name |
| `add_loan_direction_empty` | Choose lent or borrowed | Save with no Direction chosen |
| `add_loan_direction_lent_hint` | Owed to you | may reuse `home_balance_owed_to_you` instead |
| `add_loan_direction_borrowed_hint` | You owe | may reuse `home_balance_you_owe` instead |
| `add_loan_direction_borrowed_pot_hint` | Taken out of shared money | the Pot's sentence (ADR-0009) |
| `add_loan_amount` | Amount | |
| `add_loan_amount_empty` | Type the amount | Save with the Amount empty |
| `add_loan_amount_unreadable` | Type a number, such as %1$s | `%1$s` is an example Amount formatted for the Currency |
| `add_loan_amount_ambiguous` | %1$s can be read two ways. Type %2$s for a fraction, or %3$s for a whole number. | `%1$s` as typed; `%2$s` the same with a `0` added; `%3$s` with the mark replaced by a space. Typed forms, never formatted Amounts (section 1, refinement 3) |
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
| `settings_currency_picker_title` | Currency | the picker's top bar (U6); may reuse `settings_currency` |
| `settings_currency_search` | Search currencies | U6's search field |
| `settings_currency_change_title` | Change the currency to %1$s? | `%1$s` the localised currency name |
| `settings_currency_change_body` | Every amount keeps its number and is shown in %1$s. Nothing is converted at an exchange rate: %2$s becomes %3$s. | the owner's own Amount before and after |
| `settings_currency_change_confirm` | Change | |
| `settings_currency_refused_title` | The currency can't be changed | the refusal screen's top bar (U8); no currency name, so nothing needs inflecting, and no promise of editing before phase 2 |
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
| `AmountParserTest` | `app/src/test/…/ui/common/` | Every edge ADR-0008 lists, in złoty, yen and dinars; `,` `.`, a space, a no-break and a narrow no-break space as grouping; trimming; zero; the 10¹² bound; each of section 1's refinements with its examples, and both readings an `Ambiguous` carries, at three and four digits. Fails as 100,50 zł stored as 10 050 zł. |
| `RescalePlanTest` | `app/src/test/…/data/` | Relabel, multiply and divide factors in both directions; a refusal names each blocking Loan once, including one blocked only by a Repayment. Fails as 100,50 zł becoming ¥10 050. |
| `CounterpartyMatchTest` | `app/src/test/…/ui/loan/` | Trim, inner white space, Unicode case (`Łukasz`/`łukasz`, `Ola`/`OLA`), no match across diacritics (`Lukasz`/`Łukasz`). Fails as one person split into two Balances. |
| `PickerDayTest` | `app/src/test/…/ui/loan/` | The picker's UTC-midnight millis map to the same calendar day under a default zone west and east of UTC. Fails as every Loan recorded a day early. |
| `LoanDaoTest` (extended) | `app/src/androidTest/…/data/` | `addLoan` writes Counterparty and Loan together, writes only the Loan for an existing Counterparty, and writes neither when the Currency changed since parsing. `changeCurrency`, per ADR-0008's *Consequences*: every Loan's Outstanding and Settled state unchanged across an allowed change in either direction, a lossy change refused with every row as it was, a call whose `fromCode` is no longer the stored code writing nothing (a second confirm of the same multiply leaves every row at one factor), and the largest parseable Amount multiplied by 1000 without wrapping. `storedValues()` in the Counterparty screen's order, and `observeBlockingLoans` naming a Loan blocked only by a Repayment. |

No `SchemaGateTest` or migration test changes: `APP_SCHEMA_VERSION` does not move, and
`schema-gate.py` would fail the build if it did.

## Documents this phase amends

- **ADR-0008**, a dated amendment:
  - Zero is refused.
  - The whole part is below 10¹² in major units, and why that bounds the rescale (stored values below
    10¹⁵).
  - Section 1's four refinements: a lone mark before three digits is grouping only when rule 4
    allows it, and Ambiguous at three *or more* fraction digits; a zero whole part makes the mark
    decimal; trailing zeros past the fraction digits are accepted when all are `0`, in every
    Currency; a decimal separator needs a whole part.
  - Which currencies the picker offers (item 4).
  - The warning before a change is skipped when no Loan is stored (item 3). The ADR says only that
    Settings warns before the change.
- **ADR-0009**: none; item 1 was settled as its *Decision* already reads.
- **`PLAN.md`**: the phase ticked; *The values a test stands in front of* reconciled to four for
  phase 1. Item 5's answer is already in phase 2's paragraph.
- **`CONTEXT.md`**: nothing.
- **`docs/translator-brief.md`**: the §5 vocabulary rows above.
- **`CLAUDE.md`**: `ui/loan/` and `ui/currency/` in *Layout*, if a reviewer finds `ui/` alone too
  thin a hint.
- **`scripts/edge-to-edge.py`**: SCENES gain `add-loan` (from Home), `add-loan-counterparty` (from
  Ola's screen) and `settings-currency`; `screenshots.py` imports them unchanged.
- **The privacy policy, `docs/play-app-content.md` and the store listing**: nothing. No permission, no
  network, nothing collected or shared; a typed name and an Amount stay in the database that ADR-0005
  already describes.

## Known gaps, deliberately

- **A refused Currency change has no way through in this phase.** ADR-0008 sends the owner to edit the
  blocking Loans; editing is phase 2. Accepted because phase 1 reaches no stranger (the door is after
  phase 2) and the refusal loses nothing. Closing it here would mean building phase 2's edit early.
- **A Loan saved wrong stays wrong until phase 2**, for the same reason. It is why item 2 settled on no
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

This phase adds three `@Serializable` route keys, two of them with arguments (`AddLoan`,
`CurrencyRefused`), and `@Serializable` keys are what R8 breaks without a keep rule. So R2 runs on
`-PreleaseShapedDebug`, which is minified with `BuildConfig.DEBUG` false. It is still the debug
variant, so `src/debug/` and its seeder are inside it. R2 starts from a wipe and seeds nothing, so the
only rows are ones Add Loan wrote.

The plain debug build is debuggable, so its database comes off with `run-as`:

```bash
adb exec-out run-as com.maksforge.ioutracker.debug sh -c 'cd databases && tar c ioutracker.db ioutracker.db-wal ioutracker.db-shm' | tar x -C "$OUT"
```

`-PreleaseShapedDebug` sets `isDebuggable = false`, and `run-as` then answers "package not
debuggable". So R2 runs on `urgent-api26`, a `google_apis` image where `adb root` works, and pulls as
root:

```bash
adb -s <serial> root
```

```bash
for f in ioutracker.db ioutracker.db-wal ioutracker.db-shm; do adb -s <serial> pull /data/data/com.maksforge.ioutracker.debug/databases/$f "$OUT/"; done
```

| # | Reading | Command | Result |
| --- | --- | --- | --- |
| R1 | Under app language English, Currency PLN, `100.50`, `100,5`, `1,000` and `1 000` save as 10050, 10050, 100000 and 100000 minor units and show as `PLN 100.50` and `PLN 1,000.00` | Add Loan by hand; the `run-as` pull above, then `sqlite3 ioutracker.db 'SELECT amount FROM loans ORDER BY rowid'` | **Pass**, 2026-11-13, `pixel-api37` (API 37): `SELECT amount FROM loans ORDER BY rowid` → 10050, 10050, 100000, 100000. The platform's en-US currency pattern puts no space after the code, so the screen reads `PLN100.50` and `PLN1,000.00`, not `PLN 100.50`; that is the platform's formatting, not this phase's, and is a follow-up. `/tmp/claude-1000/-home-maks-repos-maks-sredniawa-iou-tracker/c9326298-ccde-40b0-a60a-852b9fa3eb19/scratchpad/readings/pixel-api37/r1-16-ola-screen.png`, `db-r1/` |
| R2 | The same four under Polish, on a release-shaped build, show as `100,50 zł` and `1000,00 zł` (or `1 000,00 zł`, as the platform groups) and store the same numbers as R1 | `./gradlew installDebug -PreleaseShapedDebug` on `urgent-api26`, app language Polish, Add Loan by hand, then the root pull above and R1's `SELECT` | **Pass**, 2026-11-13, the `urgent-api26` image (a scratch clone, API 26), release-shaped, Polish: Ola's screen shows `+1 000,00 zł` twice and `+100,50 zł` twice (no-break space). Root pull with `-wal`: `SELECT rowid, amount FROM loans` → 10050, 10050, 100000, 100000. No crash, so the route keys survive R8. `/tmp/claude-1000/-home-maks-repos-maks-sredniawa-iou-tracker/c9326298-ccde-40b0-a60a-852b9fa3eb19/scratchpad/readings/urgent-api26/r2-14-ola-screen.png` |
| R3 | A typed `ola` while `Ola` exists adds no Counterparty; a new name adds exactly one, of the kind chosen | `sqlite3 ioutracker.db 'SELECT name, kind FROM counterparties'` before and after | **Pass**, 2026-11-13, `pixel-api37` (API 37): a typed `ola` hides the Person/Pot chips and adds no Counterparty; `Kasia` as a Pot adds exactly one row, kind POT. `/tmp/claude-1000/-home-maks-repos-maks-sredniawa-iou-tracker/c9326298-ccde-40b0-a60a-852b9fa3eb19/scratchpad/readings/pixel-api37/r3-01-ola-lowercase-nochips.png`, `db-r3a/`, `db-r3b/` |
| R4 | Seeded data, PLN to JPY: refused, naming only Ola's 12,50 zł Loan, every row unchanged | seed, attempt the change, pull, `SELECT amount FROM loans; SELECT amount FROM repayments` against the seed | **Pass**, 2026-11-13, `urgent-api33` (API 33): the refusal names only Ola's Loan of Nov 2, 2026, `PLN12.50`. Loans 5000, 1250, 30000, 1500, 1500 and Repayments 2000, 10000 are identical before and after, and the Currency is still PLN. `/tmp/claude-1000/-home-maks-repos-maks-sredniawa-iou-tracker/c9326298-ccde-40b0-a60a-852b9fa3eb19/scratchpad/readings/urgent-api33/r4-05-refusal-jpy.png` |
| R5 | A wiped install, Currency JPY, a Loan of 1500, then to PLN (rows ×100, `1500,00 zł`), then back to JPY (allowed, 1500 again) | the pull and `SELECT` after each change | **Pass**, 2026-11-13, `urgent-api33` (API 33): JPY 1500 → PLN 150000 (`+PLN1,500.00` on Ola's screen; the warning reads "¥1,500 becomes PLN1,500.00") → JPY 1500. `/tmp/claude-1000/-home-maks-repos-maks-sredniawa-iou-tracker/c9326298-ccde-40b0-a60a-852b9fa3eb19/scratchpad/readings/urgent-api33/r5-06-warning-to-pln.png`, `r5-07-ola-pln.png`, `r5-08-warning-to-jpy.png` |
| R6 | Add Loan with the keyboard up keeps Save and the focused field on screen, on API 26 and API 30+ | `adb shell input tap` into Amount, `adb exec-out screencap -p > add-loan-ime-<serial>.png` | **Pass**, 2026-11-13, after dropping the Save bar's own `imePadding()`: Save and the focused Amount stay above the keyboard, from Home and from Ola, on the `urgent-api26` image (API 26, release-shaped), `small-api30` (API 30) and `pixel-api37` (API 37). `/tmp/claude-1000/-home-maks-repos-maks-sredniawa-iou-tracker/c9326298-ccde-40b0-a60a-852b9fa3eb19/scratchpad/readings/urgent-api26/r6-02-home-amount-typed.png`, `/tmp/claude-1000/-home-maks-repos-maks-sredniawa-iou-tracker/c9326298-ccde-40b0-a60a-852b9fa3eb19/scratchpad/readings/small-api30/R6-a-home-amount-focused.png`, `/tmp/claude-1000/-home-maks-repos-maks-sredniawa-iou-tracker/c9326298-ccde-40b0-a60a-852b9fa3eb19/scratchpad/readings/pixel-api37/r6-01-home-addloan-amount-focused.png` |
| R7 | A half-filled Add Loan survives process death | fill, Home button, then kill the process as root (`adb root`, `adb shell kill -9 $(adb shell pidof com.maksforge.ioutracker.debug)`; `am kill` leaves a just-backgrounded process alive on API 26), reopen from recents, screenshot | **Pass**, 2026-11-13, on the `urgent-api26` image (API 26, PID 10175 → 10682) and on `urgent-api33` (API 33, PID 5416 → 6340): Lent, `Zosia`, the Pot chip and the Amount come back. `/tmp/claude-1000/-home-maks-repos-maks-sredniawa-iou-tracker/c9326298-ccde-40b0-a60a-852b9fa3eb19/scratchpad/readings/urgent-api26/r7-04-after-restore.png`, `/tmp/claude-1000/-home-maks-repos-maks-sredniawa-iou-tracker/c9326298-ccde-40b0-a60a-852b9fa3eb19/scratchpad/readings/urgent-api33/r7-05-restored.png` |
| R8 | A system region that names no currency leaves the row null, and Add Loan hands over to Settings instead of showing the form | on an emulator: `adb root`, `adb shell setprop persist.sys.locale en-AQ`, `adb shell settings put system system_locales en-AQ`, `adb shell stop && adb shell start`, a fresh install, then Add Loan | **Pass**, 2026-11-13, `urgent-api33` (API 33): `SELECT * FROM currency` → `1|` (a NULL code). Settings says "Not set…", Add Loan shows the hand-over, and choosing PLN returns to the form. The `setprop` alone does not move the locale; `system_locales` has to be set too. `/tmp/claude-1000/-home-maks-repos-maks-sredniawa-iou-tracker/c9326298-ccde-40b0-a60a-852b9fa3eb19/scratchpad/readings/urgent-api33/r8-03-addloan-blocked.png` |
| R9 | Edge-to-edge is clean on the new scenes in every cell | `python3 scripts/edge-to-edge.py --out DIR --assert-clean --retry-unreached 2 --scene add-loan,add-loan-counterparty,settings-currency` | **Pass**, 2026-11-13, on API 26, API 30 and API 37. The first run found the *Add a loan* FAB unnamed to accessibility (`NAF="true"`), so `add-loan` and `add-loan-counterparty` were unreached; fixed by giving its icon the label. Re-run on the `urgent-api26` image: exit 0, clean, both three-button cells, all three scenes reached (gesture cells skipped below API 29). `settings-currency` was already clean in all four cells on `small-api30` and on `pixel-api37`. Re-run on `pixel-api37` (API 37): exit 0, all three scenes clean in all four cells; one touch-tier finding, a scrolling list row of `settings-currency` in landscape-gesture touching the navigation bar, nothing drawn under an inset (`/tmp/claude-1000/-home-maks-repos-maks-sredniawa-iou-tracker/c9326298-ccde-40b0-a60a-852b9fa3eb19/scratchpad/readings2/pixel-api37/e2e/report-full.json`). `/tmp/claude-1000/-home-maks-repos-maks-sredniawa-iou-tracker/c9326298-ccde-40b0-a60a-852b9fa3eb19/scratchpad/readings2/urgent-api26/e2e/report-full.json` |
| R10 | TalkBack reads each Add Loan field's label, the Direction as a choice with its state, and the Amount's error when it appears | TalkBack on, swipe through Add Loan on `pixel-api37` | **Pass for the Add Loan fields**, 2026-11-13, `pixel-api37` (API 37), TalkBack on: every field's label, the Direction as a choice with its state, and the Amount's error are read; the FAB was skipped, which the fix above addresses. The FAB now exposes `content-desc="Add a loan"` once, with no `NAF`, on API 26 (`/tmp/claude-1000/-home-maks-repos-maks-sredniawa-iou-tracker/c9326298-ccde-40b0-a60a-852b9fa3eb19/scratchpad/readings2/urgent-api26/A8-home-seeded.xml`, `A9-ola-pln.xml`), and the empty state's button is one node read once. On `pixel-api37` with TalkBack on, the fixed button reads "Add a loan. Button" on Home, on a Counterparty's screen and in the empty state (`/tmp/claude-1000/-home-maks-repos-maks-sredniawa-iou-tracker/c9326298-ccde-40b0-a60a-852b9fa3eb19/scratchpad/readings2/pixel-api37/rB-speech.log`). `/tmp/claude-1000/-home-maks-repos-maks-sredniawa-iou-tracker/c9326298-ccde-40b0-a60a-852b9fa3eb19/scratchpad/readings/pixel-api37/r10-speech.log` |
| R11 | The picker's list, per item 4: how many currencies, and whether historical ones appear | Settings → Currency on `pixel-api37` and `urgent-api26` | **Pass after the correction list**, 2026-11-13. First read on the `urgent-api26` image (ICU 58): 155 codes including the retired HRK, VEF, SLL, MRO, STD, CUC and ANG, and without MRU, STN, VES, SLE and XCG. On `pixel-api37` (API 37): 155 codes, none historical, with CNH (not ISO) and without SVC. Re-read on API 26 with the list corrected: 154 codes, none of the eight dropped, VES, SLE, MRU, STN and XCG shown by their code and SVC by name; a change to VES keeps every Amount and shows `VES12.50`. `/tmp/claude-1000/-home-maks-repos-maks-sredniawa-iou-tracker/c9326298-ccde-40b0-a60a-852b9fa3eb19/scratchpad/readings2/urgent-api26/B-currency-codes.txt`, `B4-ola-ves.png`, `/tmp/claude-1000/-home-maks-repos-maks-sredniawa-iou-tracker/c9326298-ccde-40b0-a60a-852b9fa3eb19/scratchpad/readings/pixel-api37/r11-currency-codes.tsv` |
