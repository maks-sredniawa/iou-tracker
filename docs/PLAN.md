# Roadmap

Sequence and status only. The reasoning behind a decision lives in [`docs/adr/`](adr/); the words
live in [`CONTEXT.md`](../CONTEXT.md); commands, layout and house rules live in
[`CLAUDE.md`](../CLAUDE.md).

**This file is the record, not the worklist.** What is still open — the boxes to tick, with the
device state and the commands to read it — lives in [`DOD.md`](DOD.md), which stays short enough to
open every session. Read the phase you are in here; read `DOD.md` to know what to do next.

**A phase being planned or built gets its own file** — `phase-N.md` beside this one — so working on
it costs the phase rather than the whole history. This file stays deliberately thin: one paragraph
per phase, no task lists. The detail is written when the phase opens, not now. The app this template
was extracted from and the first app built from it settled on exactly this split independently.
[`phase-template.md`](phase-template.md) is the shape a phase file settled into; copy it on the day.

## Status

- [x] **Phase 0** ([`phase-0.md`](phase-0.md)) — Skeleton: Items and photos stripped, Room, backup and reminders kept; schema 1
  in place of Items; the palette and the mark chosen; Home listing Counterparties with their Balance,
  none yet
- [x] **Phase P** — The pipeline: repository setup, upload key and its backup, Play entry, secrets;
  the first internal upload, 0.2.0, went green on 2026-11-12
- [x] **Phase 1** — Loans: record a Loan, Lent or Borrowed, with a Person or a Pot; Home lists
  Counterparties with their Balance, and a Counterparty opens to its Loans; the Currency in Settings
  (ADR-0008, ADR-0009)
- [ ] **Phase 2** — Repayments: part of a Loan paid back and listed among its Repayments, Settle,
  Settled Loans kept in the history; edit and delete ← **the door: closed testing opens here**
- [ ] **Phase 3** — Due dates and Reminders: an optional Due date and a notification on the day while
  the Loan is not Settled; the notification permission asked when the first Due date is saved
- [ ] **Phase N** — Ship shape: the Polish store listing beside English, screenshots, the App
  content answers

---

## Rules that cut across every phase

### 1. Decide where the door is, early

**The door** is the first build a stranger installs — closed testing, for a new personal Play
account. Everything that reaches a tester's phone is harder to change afterwards: a stored key, a
permission, a `minSdk`, the `applicationId`. So name the phase the door sits after, and write down what
it freezes. The closed-test window is calendar time and other people's replies, so later phases can
run while it waits.

**Do not plan a decision on the testers' answers.** The first app built from this template left two
phases' open questions — defaults, whether a behaviour was too clever — to be asked of its closed-test
testers. The survey that did go out asked none of them, and both closed *unanswered, on the values
that shipped*. What carried
the decisions was the device readings. Decide on a reading, ship a default, and let the mail hand-off
(`ui/support/`, live from the door) tell you if it is wrong.

### 2. When the two halves of a phase cost wildly different amounts, they are two phases

The first app built from this template split every one of its middle phases along cost or along
kind, because in each case a bundle of a cheap item with an expensive one — or a feature with the
safety work it depends on — hid which half was blocking. However neatly one sentence describes both, two costs are two phases.

### 3. A phase proves itself on the phone, not in a test

Every phase ends with readings taken off a real device and written into its `phase-N.md`, with the
command that produced them. A green test run says the code does what the test asked; the phone says
what the ROM allowed. Vendor ROMs are where those two part company.

**But the rule has a second clause, and leaving it out is how a roadmap arrives at no tests at all.**
Most of the risk in an app this size is device behaviour, and none of that is reachable by a unit
test — but the pure functions that *compute or bound a safety value* are the exception, and each of
them fails in exactly the direction the app exists to prevent. So: device behaviour is proven by
measurement; a safety value is proven by test, and **the phase that introduces one introduces its
test**. For the first app built from this template that came to five tests across the whole roadmap
— a window that crosses midnight, two deadlines resolved against a clock, a ramp whose bottom end
turns the backlight off over a live touchscreen, the width of a window that swallows touches, and a
restored intent that had to be refused. Not a testing culture. Five tests standing in front of the
properties `CLAUDE.md` calls load-bearing.

⚠ **Reconcile the list when the roadmap closes, and expect the count to be wrong.** That app planned
four and shipped five: the fifth arrived because a device reading found a fault no test had been
written for. That is the rule working, not an exception to it — the top of a ramp is not visible on a
device until it strands somebody.

### 4. Ask for nothing before the feature that needs it

An app of this shape ends up asking the user for three or four things, and most of them are hand-offs
to settings screens whose result cannot be read back reliably — `appops` has been caught lying about
`SYSTEM_ALERT_WINDOW` on a real phone. So the sequence is a rule rather than a phase:

**Every ask waits for the moment the feature needing it is switched on. The phase that introduces an
ask owns its hand-off, its re-read, and what the user sees when the answer is no.**

Write the table for your app — the ask, the moment it is made, the phase that owns it — and put it
here, so a later phase cannot quietly move an ask forward to "get it out of the way".

**The third clause is the one that gets left out, and it is the one that generates support mail.**
These denials nearly all fail *silently*: a denied notification permission leaves a foreground
service running with no way to stop it, a denied autostart leaves reboot restore never firing, a
denied battery exemption leaves a schedule never firing. In each case the app knows and the user does
not. A re-read tells the app; **nothing tells the person holding the phone** unless a screen does.
`ui/common/Surfaces.kt`'s `WarningBanner` is the idiom for a state the app can re-read; a permanent
row that makes no claim is the idiom for one it cannot.

**Autostart is the case where even the re-read is out of reach.** It is not an appop and there is no
API for it, so no code of yours can tell a granted state from a denied one — only
`scripts/device-gate.py`, over `adb`, run by you. The clause survives it: the phase still owns the
hand-off and still owns saying what a denial costs, without claiming to know the answer. What changes
is only who can check.

**The same logic applies to side effects, not only to permissions.** A feature that makes one of the
phone's own controls go inert is a system control that appears broken, caused by you. The phase that
introduces the behaviour owns saying so.

---

## The door

Closed testing opens after phase 2: from then a tester can lend, borrow, repay and settle, and
backup restores all of it. What it freezes: `com.maksforge.ioutracker` and `minSdk` 26.

The schema freezes earlier, at Phase P's first upload: the release build carries migrations from
that build on, because `destructiveMigrationAllowed()` is debug-only (ADR-0001). So schema 1 is
written whole in phase 0, Due date included, and phases 1–3 owe no migration.

`POST_NOTIFICATIONS` and `RECEIVE_BOOT_COMPLETED` are declared from phase 0, with the reminder module
the template ships; declaring asks nothing. Phase 3 adds the ask, which is behaviour, not a new
permission.

## What the app asks for

| The ask | The moment it is made | Phase |
| --- | --- | --- |
| `POST_NOTIFICATIONS` (Android 13+) | the first time a Due date is saved, never at launch | 3 |

Nothing else. No contacts (ADR-0009), no network, and no exact alarms: a Reminder is a day, which
WorkManager's daily sweep covers, so phase 0 removes `SCHEDULE_EXACT_ALARM` and Settings' template
*Reminders* ask (ADR-0003, amended then).

## The values a test stands in front of

Per rule 3: parsing a typed Amount and rescaling every Amount when the Currency changes (ADR-0008),
matching a typed name to its Counterparty, and turning the date picker's millis into a Day (phase 1,
four tests where two were planned: `docs/phase-1.md`, *Tests*); Outstanding never going below zero and a Balance's sign (phase 2); which day a Reminder
fires on across a time-zone change (phase 3).

## Phase 0 — Skeleton

The template's Items and photo pipeline go, per `docs/optional-modules.md`; Room, backup and
the reminder module stay, because phases 1–3 need all three. Schema 1 (Counterparty with its `kind`,
Loan with a nullable Due date, Repayment, the Currency row) replaces `ItemEntity` before Phase P's
first upload. Palette and mark chosen, the Tip ask moved to Home, Polish kept complete from here on,
`edge-to-edge.py` rewritten against Home. The app opens on Home with no Counterparties yet.

## Phase P — The pipeline

`DOD.md`'s *Before the first upload*: repository ruleset, upload key and its backup, the Play entry
for `com.maksforge.ioutracker`, the secrets, and a green upload to the internal track.

## Phase 1 — Loans

Screens on phase 0's schema, no migration. Add a Loan: who, which Direction, Amount, the day; it
writes the Loan and, for a new name, its Counterparty. Home lists Counterparties with their Balance;
a Counterparty opens to its Loans. The Currency in Settings, the exact rescale on a change, the
warning before it and the refusal of a change that would lose a fraction (ADR-0008). Reading:
amounts typed in Polish and English locales come back unchanged.

## Phase 2 — Repayments

Record a Repayment of any part of what is Outstanding; Settle records the rest. Settled Loans move to
the history. Editing and deleting a Loan, the delete asking first; renaming a Counterparty, switching
it between Person and Pot, and deleting one with no Loans. Backup restore read on a second emulator.
Closed testing opens.

## Phase 3 — Due dates and Reminders

An optional Due date on a Loan; on that day, a Reminder while it is not Settled, surviving a reboot.
The ask and its denial per the table above. Readings: the Reminder fires on an emulator after a
reboot and after the clock crosses midnight, with the WorkManager sweep (`reminder-sweep`) read as
pending before and after each, off `adb shell dumpsys jobscheduler` or WorkManager's diagnostics
broadcast, rather than an alarm off `dumpsys alarm`.

## Phase N — Ship shape

The Polish store listing beside the English one, screenshots, Data safety (nothing collected,
nothing shared), and the App content answers. The app's own Polish copy is not here: it has shipped
with every phase since phase 0, because `translation-gate.py` holds each merge to it.

## Not in this plan

- Accounts, sync or a server: every record stays on the phone, so there is no account to lose and
  nothing to declare in Data safety.
- Telling the other person: no messages, links or shared Loans. Each needs the network or a second
  install, and the app has neither.
- Interest or instalment schedules: a loan between friends carries neither, and partial Repayments
  already cover paying back in parts.
- More than one Currency: every Balance would have to be shown per currency (ADR-0008).
- Splitting group expenses or tracking who is in a Pot: that is a group-expense app, and without
  members a Pot works exactly like a Person (ADR-0009).
- Reading the phone's contacts: a sensitive permission for a list that is mostly people the owner
  never lends to (ADR-0009).
