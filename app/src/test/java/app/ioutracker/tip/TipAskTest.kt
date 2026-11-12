package app.ioutracker.tip

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TipAskTest {
    private val installedAt = Instant.parse("2026-09-01T08:00:00Z")
    private val monthLater = installedAt + TIP_ASK_AFTER

    private fun due(
        asked: Boolean = false,
        loans: Int = TIP_ASK_AFTER_LOANS,
        now: Instant = monthLater,
    ) = tipAskDue(asked, loans, installedAt, now)

    @Test
    fun `due once installed a month with ten Loans recorded`() {
        assertTrue(due())
    }

    @Test
    fun `not due a millisecond short of the month`() {
        assertFalse(due(loans = 1_000, now = monthLater.minusMillis(1)))
    }

    @Test
    fun `not due one Loan short`() {
        assertFalse(due(loans = TIP_ASK_AFTER_LOANS - 1, now = monthLater.plus(TIP_ASK_AFTER)))
    }

    @Test
    fun `never again once answered`() {
        assertFalse(due(asked = true, loans = 1_000, now = monthLater.plus(TIP_ASK_AFTER)))
    }
}
