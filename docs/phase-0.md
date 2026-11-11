# Phase 0 — The app opens on an empty Loan list, with its own mark and colours

## 0. What this phase inherits

The bootstrapped template (PR #1): Items, the photo pipeline, Room, backup, reminders, the Tip.

## What is in, and what is deliberately not

**In:** the Item domain and the photo pipeline stripped per `docs/optional-modules.md`; Room, backup
and the reminder module kept, because phases 1–3 need them; the mark and the palette below; one Home
screen showing an empty Loan list; `scripts/edge-to-edge.py`'s SCENES and
`scripts/aab-permissions.py`'s allowlists rewritten against what is left.

**Not:** any Loan, Counterparty or Repayment table (phase 1); the Reminder's ask (phase 3).

## The mark: Tally

Four strokes and a strike through them: keeping score with a friend, and the strike reads as Settled.
Chosen 2026-11-11 over a split coin and a torn stub. In `art/mark.py` units:

- Four vertical rounded bars, 18 wide, 120 tall, radius 9, centred at x = -54, -18, 18, 54, y from
  -60 to 60.
- A hole: a rounded bar 34 thick, 200 long, centred on the origin, rising left to right at 24°.
- Declared after the hole, the strike: same centre and angle, 18 thick, 190 long, radius 9. The
  8-unit gap around it is a real hole, so the crossing survives the monochrome themed icon and the
  24dp notification icon.
- `GROUND` is the light scheme's `primary` (tone 40 indigo), `MARK_INK` its `surface`.

## The palette

| Seed | Hex | |
| --- | --- | --- |
| `SEED_PRIMARY` | `4B55A6` | twilight indigo |
| `SEED_SECONDARY` | `666A82` | slate lavender |
| `SEED_TERTIARY` | `B07F2A` | honey ochre |
| `SEED_NEUTRAL` | `E1E0E8` | cool grey, faint violet |

Lent is drawn in `primary`, Borrowed in `tertiary`, and red stays reserved for an overdue Due date.
Blue against yellow survives the common red-green deficiencies, but both sit at the same tone, so
colour is never the only cue: the word Lent or Borrowed, an arrow (up-right out, down-left in) and
the sign on the Amount always appear, and Lent's chip is filled where Borrowed's is outlined. The
`gen_scheme.py` contrast report is read when the seeds go in.

## Readings

*(the honey ochre on the dark scheme's tone-6 surface, read on an emulator: it can turn olive; the
themed icon at 48dp; edge-to-edge across the emulator matrix)*
