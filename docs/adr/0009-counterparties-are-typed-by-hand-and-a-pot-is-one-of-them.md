# Counterparties are typed by hand, and a Pot is a kind of Counterparty

## Context

Every Loan is with someone or something: a friend, a sibling, the shared savings. The two obvious
shortcuts are to read the phone's contacts, and to model shared money as a separate feature.

## Decision

**A Counterparty is a row the owner creates by typing a name.** The app never asks for
`READ_CONTACTS`. Picking a name already used offers the existing Counterparty, so one person does
not split into two Balances over a typo.

**A Pot is a Counterparty with `kind = POT`**, beside `kind = PERSON`. Loans, Repayments, Outstanding
and Balance work the same for both; only the icon and a few sentences differ ("taken from the joint
savings" rather than "borrowed from Ola"). The `kind` column is in the first schema, so adding Pots
never needs a migration.

## Alternatives

- **Pick from the phone's contacts.** Saves typing a name once per person, and costs a runtime
  permission whose Play Data safety answer is the most sensitive one an app this size could give,
  for a list that is mostly people the owner never lends to. A contact's ID is also not stable across
  a phone change, which a restored backup would meet. Lost on the permission.
- **Pots as their own table and screens.** Shared money does differ from a person (several people can
  stand behind one Pot), but tracking who is in a Pot is group expense splitting, which this app does
  not do (PLAN.md, *Not in this plan*). Without members, a Pot behaves exactly like a person, so a
  second model would be two copies of the same code.

## Consequences

- No permission, no Data safety entry for contacts, nothing to explain at first launch.
- The owner types each name once. Renaming a Counterparty renames it on every Loan.
- If the app ever wants to know who is in a Pot, that is a new decision that supersedes this one.
