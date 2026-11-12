# Phase 0 — The app opens on an empty Home, with its own mark, colours and first schema

## 0. What this phase inherits

The bootstrapped template (PR #1): Items, the photo pipeline, Room, backup, reminders, the Tip, and
Polish beside English (`locales_config.xml` lists `pl`, so `scripts/translation-gate.py` holds every
merge to complete Polish from this phase on).

## What is in, and what is deliberately not

**In:**

- The strip, per `docs/optional-modules.md` in full: the placeholder domain (`ui/items/`, its
  routes, strings, the debug seeding pointed at Loans or removed) and the photo pipeline
  (`media/MediaFiles.kt`, its test, `file_paths.xml` and the `FileProvider`, `coil.compose` and both
  `exifinterface` lines). Room, backup and the reminder module stay, because phases 1–3 need all
  three.
- **Schema 1** in place of `ItemEntity`: `CounterpartyEntity` (name, `kind` PERSON or POT, ADR-0009),
  `LoanEntity` (Counterparty, Direction, Amount in minor units, the day it was made, a nullable Due
  date, ADR-0008), `RepaymentEntity` (Loan, Amount, day), and the one-row Currency table (ADR-0008),
  filled from the system region when the database is created, with its exported
  `app/schemas/…/1.json`. It ships in Phase P's first upload, and the release build takes
  migrations from that upload on: `destructiveMigrationAllowed()` is debug-only (ADR-0001).
  Phases 1–3 build screens on this schema and add no table and no column.
- **Home** as it stays: the Counterparties with their Balance, none yet; a Counterparty opens to its
  Loans. `scripts/edge-to-edge.py`'s SCENES, `TAB_BAR`, `HOME_TAB` and `SEED_WALK` walk it.
- **The Tip ask moves to Home** (`TipAskCard.kt` and its `items_tip_ask_*` strings, renamed for
  Home), and `tipAskDue` counts Loans instead of items: ten Loans and thirty days.
- **The reminder module without its template ask**: `work/ExactAlarms.kt`, `SCHEDULE_EXACT_ALARM`
  and its `aab-permissions.py` EXPECTED row go, because a Reminder is a day and WorkManager's daily
  sweep covers a day (ADR-0003). `scripts/device-gate.py` is built around AlarmManager, and phase 0
  reduces it to what stays true without exact alarms: the autostart, battery-exemption and channel
  readings and the reboot-and-unlock wait stay; `ALARM_TAG`, `AppAlarm` (exact against best-effort
  by its window), `app_alarms`, `armed`, `disarmed` and the alarm lines `main` prints go, and
  `reboot_and_wait`'s premise that only `BootReceiver` can re-arm anything after boot goes with
  them, since WorkManager restores its own work. Phase 3's reboot and midnight readings observe the
  sweep (`reminder-sweep`) instead, through `adb shell dumpsys jobscheduler` or WorkManager's
  diagnostics broadcast. Settings' *Reminders* section goes with the ask behind it (the
  `SwitchRow`, `rememberNotificationPermissionAsk`, the re-read, the `WarningBanner`, the
  `settings_reminders*` strings, `remindersEnabled` and "reminders" in the restore strings), read
  first, because it is the shape phase 3's ask copies. `POST_NOTIFICATIONS`
  and `RECEIVE_BOOT_COMPLETED` stay declared: declaring asks nothing, and the sweep and its boot
  receiver are what phase 3 schedules on.
- The mark and the palette below; `scripts/aab-permissions.py`'s allowlists rewritten against what
  is left.

**Not in, and each with a reason rather than a phase:**

- Adding a Loan, or any screen that writes a row. The schema is what has to be right before the
  first upload, because every later change to it is a migration; a screen can change in any build.
- The notification ask. Nothing in this phase schedules a Reminder, and rule 4 makes the ask wait for
  the first saved Due date.
- The Currency in Settings, and the rescale when it changes. With no Amount stored there is nothing
  to label or rescale, and the fallback for a region with no currency is asked on the first Add Loan
  (ADR-0008).

## The mark: Tally

Four strokes and a strike through them: keeping score with a friend, and the strike reads as Settled.
Chosen 2026-11-11 over a split coin and a torn stub. In `art/mark.py` units, nine solids and no
hole:

- Four vertical bars, 18 wide, centred at x = -54, -18, 18, 54, y from -60 to 60, each with a
  radius-9 cap at its outer end. **Each bar is two solids**, above and below the strike's band: the
  band is 34 thick, centred on the origin and rising left to right at 24°, and each piece ends in a
  straight cut along the band's edge.
- The strike: a rounded bar 18 thick, 190 long, radius 9, on the same centre and angle, declared as
  an ordinary solid. It sits 8 units clear of every cut, and its ends reach past the outer bars.
- Every part is wound clockwise and none overlaps another, so `make-launcher-icon.py`'s single
  non-zero path (launcher, themed and 24dp notification icons) and the PIL rasters (Play icon,
  feature graphic) draw the same thing, and the gap survives the monochrome layer because it is
  empty, not painted.
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

## Documents this phase amends

- `docs/privacy-policy.md` and `docs/play-app-content.md`, in one commit: the *Photos* and *camera*
  bullets and Data safety point 2 go.
- ADR-0002, a dated amendment: the media pipeline is gone with the photos.
- ADR-0005, a dated amendment: the thumbnail half of the backup is gone; the database half stays.
- ADR-0003, a dated amendment: no exact alarms, the daily sweep alone carries a day-granularity
  Reminder, and the notification ask moves from Settings to the first saved Due date.
- ADR-0001: none. The schema it gates is still there, and is now schema 1.
- `docs/play-app-content.md`: the `SCHEDULE_EXACT_ALARM` row of the permission table.
- `CLAUDE.md`: the Photo Picker row and the "WorkManager + exact alarms" row in Stack (WorkManager
  alone), the three media house rules, `media/` in Layout, and the `ui/items/` line.
- `CONTEXT.md`: nothing; its words are already the schema's.

## Readings

*(the honey ochre on the dark scheme's tone-6 surface, read on an emulator: it can turn olive; the
themed icon at 48dp; edge-to-edge across the emulator matrix)*

- 2026-11-12, `pixel-api37` (API 37, 420dpi), dark mode, a debug-only swatch on Settings: `tertiary`
  `#F6BD62` on `surface` `#121318` reads as honey amber, **not olive**, as text, a filled square and
  an outlined chip, beside `primary` `#BBC2FF`. The pixels match the generated scheme exactly. The
  dark scheme's `tertiaryContainer` (`#614000`) reads brown; it was not on screen.
- 2026-11-12, `pixel-api37` (API 37, 420dpi), themed icons on (Wallpaper & style, Icons, style
  *Minimal*: API 37 has no "Themed icons" toggle), light and dark: the launcher draws the monochrome
  layer tinted like its neighbours, and the four bars, the strike and the gap are all distinct. Read
  at the launcher's own **61dp** (159px), where the gap is about 2dp. **The 48dp size itself is not
  yet read**: no other API 33+ phone was free for it.
