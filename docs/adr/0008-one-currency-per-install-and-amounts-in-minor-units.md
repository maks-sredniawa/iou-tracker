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

**Changing the Currency rescales every stored Amount and Repayment** by 10^(new digits − old
digits), in one transaction with the row's change. When digits are dropped, the result is rounded
half-even. Between currencies with the same digits that is a factor of one: the numbers are
re-labelled, never converted at an exchange rate. Settings warns before the change.

**Parsing what the owner types** takes the last `,` or `.` as the decimal separator when at most
`defaultFractionDigits` digits follow it; every other `,`, `.` and space is grouping and ignored.
So `100,50`, `100.50` and `100.5` are the same Amount in złoty, `1.000` and `1 000` are a thousand,
and the rule does not depend on the app's language or the phone's. Formatting what the owner sees
goes through the in-app language.

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
- **One fixed scale for every install** (always two digits, formatting rounds). Simpler than a
  rescale, but a dinar's third digit could not be stored, and a yen Amount would hold hundredths the
  currency does not have. Lost on storing numbers that are not the Currency's own.
- **`BigDecimal` in the schema.** Room stores it as text or needs a converter, sums cannot happen in
  SQL, and nothing an informal loan needs is beyond a `Long` of minor units.

## Consequences

- A loan in a second currency has to be entered as its value in the app's Currency, by hand.
- Changing the Currency after Loans exist rescales and re-labels them, and dropping digits can lose
  a fraction of a minor unit per row. Settings says so before the change is made.
- Two safety values under PLAN.md rule 3, each with a unit test in the phase that introduces it:
  parsing a typed Amount (a misread separator turns 100,50 zł into 10 050 zł), and the rescale on a
  Currency change (a missed factor turns 100,50 zł into ¥10,050).
