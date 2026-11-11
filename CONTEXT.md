# Vocabulary

**The rule this file exists to enforce: one concept, one word, everywhere.** The entity, the DAO, the
route key, the string resource and the sentence in the UI all use it. When the domain word and the
user-facing word genuinely differ, both are written here with the difference stated.

## The domain

| Term | Means | Not |
| --- | --- | --- |
| **Loan** | one sum of money that changed hands once, between the owner and one Counterparty, in one Direction | debt, IOU (the app's name only), transaction, entry |
| **Direction** | which way a Loan went: **Lent** (the Counterparty owes the owner) or **Borrowed** (the owner owes the Counterparty) | given/taken, credit/debit, in/out |
| **Counterparty** | whoever is on the other side of a Loan: a Person or a Pot. A code word; the UI asks *"Who?"* and shows the name | contact, friend, account |
| **Person** | a Counterparty that is somebody, with a name the owner typed | contact (the phone's contacts are never read, ADR-0009) |
| **Pot** | a Counterparty that is shared money rather than somebody, such as joint savings. Money taken out of a Pot is Borrowed from it | account, wallet, fund, group |
| **Amount** | what a Loan was for when it was made. Never changes because of a Repayment | principal, total |
| **Day** | the date a Loan was made, or a Repayment paid: a calendar date, no time of day | timestamp, created at |
| **Repayment** | money paid back against one Loan, any part of what is Outstanding | payment, instalment, transfer |
| **Outstanding** | a Loan's Amount minus its Repayments; never below zero | remaining, left, balance |
| **Settled** | a Loan whose Outstanding is zero. It stays in the history | closed, paid off, done, archived |
| **The history** | the Settled Loans, kept and shown apart from the open ones | archive, closed Loans, past |
| **Balance** | one Counterparty's Outstanding Lent minus Outstanding Borrowed. Positive: they owe the owner | net, total, score |
| **Due date** | an optional day by which a Loan should be Settled | deadline, term |
| **Reminder** | the notification on a Loan's Due date while it is not Settled | alarm, alert |
| **Currency** | the one currency every Amount is in, stored with the data, taken from the phone's region and changeable in Settings (ADR-0008) | |
| **Tip** | a one-off payment to the developer through Google Play that **unlocks nothing** (ADR-0007). The home screen mentions it once per install; Help and feedback offers it for good | donation, support the project, premium, supporter |

## Words we deliberately avoid

- **"Debt" and "owe" without a Direction.** "Debt" reads one way to the lender and the other to the
  borrower; every sentence that needs it says *who owes whom* instead.
- **"Paid" for a Loan.** A Loan is Settled; only a Repayment is paid. "Mark as paid" is
  *Settle*, which records a Repayment for whatever is Outstanding, so the history still adds up.
- **"Delete" is irreversible and asks first.** A Settled Loan is not deleted, it stays in the
  history; deleting is for a Loan entered by mistake.

## Naming conventions in code

Some names in this codebase are deliberately generic — `AppDatabase`, `AppTheme`, `AppPreferences`,
`AppContainer`, `MainApplication`, `MainActivity`. They stay correct whatever the app is called, so
renaming the app is a package move rather than a repo-wide find-and-replace. Don't rename them to
match the product; name the *domain* types after the domain instead.
