package app.ioutracker.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RescalePlanTest {
    private val zloty = 2
    private val yen = 0
    private val dinar = 3

    private fun amount(
        loanId: String,
        minorUnits: Long,
    ) = StoredValue(loanId, minorUnits)

    private val whole =
        listOf(
            amount("ola-1", 10_000),
            amount("ola-1", 2_500),
            amount("pot-1", 30_000),
        )

    @Test
    fun `the same digits re-label, whatever is stored`() {
        assertEquals(RescalePlan.Relabel, planRescale(zloty, zloty, whole + amount("ola-2", 1_250)))
        assertEquals(RescalePlan.Relabel, planRescale(yen, yen, emptyList()))
    }

    @Test
    fun `more digits multiply by ten to the difference`() {
        assertEquals(RescalePlan.Multiply(100), planRescale(yen, zloty, whole))
        assertEquals(RescalePlan.Multiply(10), planRescale(zloty, dinar, whole))
        assertEquals(RescalePlan.Multiply(1_000), planRescale(yen, dinar, whole))
    }

    @Test
    fun `more digits multiply even when a value has a fraction`() {
        assertEquals(RescalePlan.Multiply(10), planRescale(zloty, dinar, listOf(amount("ola-2", 1_250))))
    }

    @Test
    fun `fewer digits divide when every value is whole at the new scale`() {
        assertEquals(RescalePlan.Divide(100), planRescale(zloty, yen, whole))
        assertEquals(RescalePlan.Divide(10), planRescale(dinar, zloty, listOf(amount("a", 1_230))))
        assertEquals(RescalePlan.Divide(1_000), planRescale(dinar, yen, listOf(amount("a", 7_000))))
    }

    @Test
    fun `fewer digits with nothing stored divide`() {
        assertEquals(RescalePlan.Divide(100), planRescale(zloty, yen, emptyList()))
    }

    @Test
    fun `a fraction the new Currency cannot hold refuses, naming that Loan`() {
        val values = whole + amount("ola-2", 1_250)
        assertEquals(RescalePlan.Refused(listOf("ola-2")), planRescale(zloty, yen, values))
    }

    @Test
    fun `a Loan blocked only by a Repayment is named`() {
        val values =
            listOf(
                amount("ola-1", 10_000),
                amount("ola-1", 3_333),
                amount("pot-1", 30_000),
            )
        assertEquals(RescalePlan.Refused(listOf("ola-1")), planRescale(zloty, yen, values))
    }

    @Test
    fun `each blocking Loan is named once, in the order it first appears`() {
        val values =
            listOf(
                amount("b", 1_250),
                amount("a", 10_000),
                amount("a", 1),
                amount("b", 99),
                amount("c", 5),
                amount("a", 7),
            )
        assertEquals(RescalePlan.Refused(listOf("b", "a", "c")), planRescale(zloty, yen, values))
    }

    @Test
    fun `a value divisible by ten but not by a hundred blocks a two-digit drop only`() {
        val values = listOf(amount("a", 1_230))
        assertEquals(RescalePlan.Divide(10), planRescale(dinar, zloty, values))
        assertEquals(RescalePlan.Refused(listOf("a")), planRescale(dinar, yen, values))
    }

    @Test
    fun `an allowed change there and back returns every value`() {
        val there = planRescale(yen, dinar, whole) as RescalePlan.Multiply
        val scaled = whole.map { it.copy(minorUnits = it.minorUnits * there.factor) }
        val back = planRescale(dinar, yen, scaled) as RescalePlan.Divide
        assertEquals(whole, scaled.map { it.copy(minorUnits = it.minorUnits / back.factor) })
    }

    private data class Loan(
        val id: String,
        val amount: Long,
        val repayments: List<Long>,
    ) {
        val outstanding get() = maxOf(amount - repayments.sum(), 0L)
        val settled get() = outstanding == 0L
        val values get() = (listOf(amount) + repayments).map { StoredValue(id, it) }

        fun scaled(scale: (Long) -> Long) = copy(amount = scale(amount), repayments = repayments.map(scale))
    }

    private val loans =
        listOf(
            Loan("settled-in-thirds", 900, listOf(300, 300, 300)),
            Loan("half-repaid", 1_000, listOf(500)),
            Loan("overpaid", 400, listOf(300, 200)),
            Loan("untouched", 7_000, emptyList()),
        )

    private fun assertKeepsOutstandingAndSettled(
        before: List<Loan>,
        after: List<Loan>,
        toNewScale: (Long) -> Long,
    ) {
        before.zip(after).forEach { (old, new) ->
            assertEquals(old.id, toNewScale(old.outstanding), new.outstanding)
            assertEquals(old.id, old.settled, new.settled)
        }
    }

    @Test
    fun `Outstanding and Settled survive an allowed change in either direction`() {
        val up = planRescale(yen, dinar, loans.flatMap { it.values }) as RescalePlan.Multiply
        val multiplied = loans.map { loan -> loan.scaled { it * up.factor } }
        assertKeepsOutstandingAndSettled(loans, multiplied) { it * up.factor }

        val down = planRescale(zloty, yen, loans.flatMap { it.values }) as RescalePlan.Divide
        val divided = loans.map { loan -> loan.scaled { it / down.factor } }
        assertKeepsOutstandingAndSettled(loans, divided) { it / down.factor }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative digits are a programming error`() {
        planRescale(-1, zloty, emptyList())
    }
}
