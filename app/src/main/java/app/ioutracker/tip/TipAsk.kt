package app.ioutracker.tip

import java.time.Duration
import java.time.Instant

/** Loans recorded before the tip is mentioned. */
const val TIP_ASK_AFTER_LOANS = 10

/** How long the app has to have been installed before the tip is mentioned. */
val TIP_ASK_AFTER: Duration = Duration.ofDays(30)

/**
 * Whether the home screen should mention the tip — **once per install, to someone who already uses
 * the app** (ADR-0007).
 *
 * [installedAt] is the package's `firstInstallTime`, which a restore onto a new phone does not carry,
 * so a moved install waits its own month. [asked] lives in preferences, which `AppBackupAgent` leaves
 * out (ADR-0005), so a user who answered is asked once more on their next phone, after that month.
 */
fun tipAskDue(
    asked: Boolean,
    loans: Int,
    installedAt: Instant,
    now: Instant,
): Boolean =
    !asked &&
        loans >= TIP_ASK_AFTER_LOANS &&
        Duration.between(installedAt, now) >= TIP_ASK_AFTER
