# One currency per install, and every Amount is a whole number of minor units

## Context

An informal loan is almost always in the owner's own money: 100 zł to a friend, 300 zł out of joint
savings. A loan in a second currency happens (a holiday abroad), but rarely enough that the cost of
supporting it has to be weighed against everyone else's screens.

Money also cannot be a floating-point number. `0.1 + 0.2` is not `0.3` in a `Double`, and a Balance
summed over a year of Repayments drifts by visible grosze.

## Decision

**The app has one Currency, and its code is data.** It lives in a one-row table in schema 1, not in
`AppPreferences`, so it travels with the rows it gives meaning to: the backup, the export and a
restore onto a phone in another region all carry it (ADR-0005 leaves preferences out). It is taken
from the **system region** when the database is created (`LocaleManagerCompat.getSystemLocales`,
not `Locale.getDefault()`, which ADR-0004 turns into the in-app language and whose `en` and `pl`
carry no region). When the region names no currency, the row stays empty and the first Add Loan asks
for one in Settings. It can be changed in Settings later.

**Every Amount and Repayment is stored as a `Long` of minor units at the Currency's
`defaultFractionDigits`** (grosze, cents; none for yen, three for dinars).

**Changing the Currency rescales every stored Amount and Repayment exactly, and never rounds.**
A Currency with more fraction digits multiplies every row by 10^(new − old) in one transaction with
the Currency row's change. A Currency with fewer is refused while any stored Amount or Repayment has
a non-zero fraction it would lose (a value not divisible by 10^(old − new)): Settings says why and
lists the Loans that stand in the way, and the owner edits them or keeps the Currency. With no such
value, every row is divided by 10^(old − new), in the same single transaction. Between currencies
with the same digits the factor is one: the numbers are re-labelled, never converted at an exchange
rate. Every value maps to the same amount at the new scale, so every Loan keeps its Outstanding and
stays Settled or open. Settings warns before the change.

**Parsing what the owner types** refuses anything it would have to guess: the field shows an error
and nothing is saved. The rule, independent of the app's language and the phone's:

1. After trimming, only digits, `,`, `.` and spaces (a no-break or narrow no-break space too) are
   accepted, with at least one digit. No sign: the Direction carries it.
2. The candidate decimal separator is the last `,` or `.`. It is grouping when its mark occurs more
   than once. Otherwise it is the decimal separator when a different separator (the other mark or a
   space) comes before it, or when what follows it is anything but exactly three digits. A lone mark
   followed by exactly three digits is grouping when the Currency has fewer than three fraction
   digits, and is refused as ambiguous when it has three.
3. What follows a decimal separator is 1 to `defaultFractionDigits` digits and nothing else; more
   digits, none, or anything else is refused.
4. The whole part uses at most one grouping mark, and it splits the digits into a first group of one
   to three and then groups of exactly three; any other grouping is refused.

So `100,50`, `100.50` and `100.5` are the same Amount in złoty, `1.000`, `1,000` and `1 000` are a
thousand, `100.5` is refused for yen, `1,000` is refused as ambiguous for dinars while `1.000,000` and
`1 000,000` are a thousand dinars, and `100,505` is a hundred thousand five hundred and five złoty.
The parsing unit test covers each of these edges. Formatting what the owner sees goes through the
in-app language.

## Alternatives

- **A Currency per Loan.** Each Loan would carry its own code, and every Balance and total would have
  to be shown per currency, because adding złoty to euro without an exchange rate is meaningless.
  That doubles the home screen and the per-Counterparty screen for a case most owners never hit.
  Lost on cost; the schema can still grow a column later, because a missing code reads as the
  install's Currency.
- **Exchange rates.** Needs the network, which this app does not have (CLAUDE.md), and a rate on which
  day? The day the Loan was made, or the day it is repaid? Friends settle in the money they lent.
- **The Currency in `AppPreferences`**, where a setting usually goes. The backup and the export
  leave preferences out (ADR-0005), so a restore would re-derive the Currency on the new phone and
  re-label every restored Amount. Lost on that.
- **Rounding each value on its own** (half-even) when digits are dropped. It changes Outstanding:
  10,00 zł repaid as 3,33 + 3,33 + 3,34 becomes ¥10 against ¥3 + ¥3 + ¥3, a Settled Loan with ¥1
  Outstanding, and 10,00 zł with 9,50 repaid becomes ¥10 against ¥10, Settled while 0,50 zł was
  owed. Lost on reopening and closing Loans.
- **Rounding per Loan** when digits are dropped: the Amount, then each Repayment as the difference of
  its rounded running total, a non-zero Outstanding kept at one minor unit at least. Keeps every
  Loan's state, but it is careful code and tests for a change almost nobody makes once Loans with
  fractions exist, and the refusal leaves the few blocking Loans to the owner. Lost on complexity
  for a rare case.
- **One fixed scale for every install** (always two digits, formatting rounds). Simpler than a
  rescale, but a dinar's third digit could not be stored, and a yen Amount would hold hundredths the
  currency does not have. Lost on storing numbers that are not the Currency's own.
- **`BigDecimal` in the schema.** Room stores it as text or needs a converter, sums cannot happen in
  SQL, and nothing an informal loan needs is beyond a `Long` of minor units.

## Consequences

- A loan in a second currency has to be entered as its value in the app's Currency, by hand.
- Changing the Currency after Loans exist rescales and re-labels them. A change to fewer fraction
  digits waits until no stored value has a fraction it would lose; Settings names the Loans in the
  way.
- Two safety values under PLAN.md rule 3, each with a unit test in the phase that introduces it:
  parsing a typed Amount (a misread separator turns 100,50 zł into 10 050 zł), covering every edge
  above, and the rescale on a Currency change (a missed factor turns 100,50 zł into ¥10,050). The
  rescale test asserts that every Loan's Outstanding and Settled state are unchanged across an
  allowed change in either direction, and that a lossy change is refused and leaves every row as it
  was.
