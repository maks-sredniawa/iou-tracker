# One currency per install, and every Amount is a whole number of minor units

## Context

An informal loan is almost always in the owner's own money: 100 zł to a friend, 300 zł out of joint
savings. A loan in a second currency happens (a holiday abroad), but rarely enough that the cost of
supporting it has to be weighed against everyone else's screens.

Money also cannot be a floating-point number. `0.1 + 0.2` is not `0.3` in a `Double`, and a Balance
summed over a year of Repayments drifts by visible grosze.

## Decision

**The app has one Currency.** It is taken from the phone's locale on first launch
(`Currency.getInstance(Locale.getDefault())`) and can be changed in Settings. Changing it re-labels
every Amount; it never converts anything.

**Every Amount and Repayment is stored as a `Long` of the Currency's minor units** (grosze, cents),
using `Currency.defaultFractionDigits`. Parsing what the owner types and formatting what they see go
through the locale, so `100,50` and `100.50` both mean what the phone's language means by them.

## Alternatives

- **A Currency per Loan.** Each Loan would carry its own code, and every Balance and total would have
  to be shown per currency, because adding złoty to euro without an exchange rate is meaningless.
  That doubles the home screen and the per-Counterparty screen for a case most owners never hit.
  Lost on cost; the schema can still grow a column later, because a missing code reads as the
  install's Currency.
- **Exchange rates.** Needs the network, which this app does not have (CLAUDE.md), and a rate on which
  day? The day the Loan was made, or the day it is repaid? Friends settle in the money they lent.
- **`BigDecimal` in the schema.** Room stores it as text or needs a converter, sums cannot happen in
  SQL, and nothing an informal loan needs is beyond a `Long` of minor units.

## Consequences

- A loan in a second currency has to be entered as its value in the app's Currency, by hand.
- Changing the Currency in Settings after Loans exist re-labels them. Settings says so before the
  change is made.
- Parsing typed amounts is a safety value under PLAN.md rule 3: it gets a unit test in the phase that
  introduces it (a misread decimal separator turns 100,50 zł into 10 050 zł).
