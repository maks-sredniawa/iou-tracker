package app.ioutracker.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class AmountParserTest {
    private val zloty = 2
    private val yen = 0
    private val dinar = 3
    private val unitOfAccount = 4

    private fun valid(minorUnits: Long) = ParsedAmount.Valid(minorUnits)

    private fun assertParses(
        expected: ParsedAmount,
        typed: String,
        digits: Int,
    ) = assertEquals("\"$typed\" at $digits digits", expected, parseAmount(typed, digits))

    @Test
    fun `either mark is the decimal separator in zloty`() {
        assertParses(valid(10_050), "100,50", zloty)
        assertParses(valid(10_050), "100.50", zloty)
        assertParses(valid(10_050), "100.5", zloty)
        assertParses(valid(10_050), "100,5", zloty)
        assertParses(valid(10_000), "100", zloty)
    }

    @Test
    fun `a lone mark before three digits is grouping below three fraction digits`() {
        assertParses(valid(100_000), "1.000", zloty)
        assertParses(valid(100_000), "1,000", zloty)
        assertParses(valid(10_050_500), "100,505", zloty)
        assertParses(valid(1_000), "1.000", yen)
        assertParses(valid(1_000), "1,000", yen)
    }

    @Test
    fun `every kind of space is grouping`() {
        assertParses(valid(100_000), "1 000", zloty)
        assertParses(valid(100_000), "1 000", zloty)
        assertParses(valid(100_000), "1 000", zloty)
        assertParses(valid(100_000_000), "1 000 000", zloty)
        assertParses(valid(100_050), "1 000,50", zloty)
    }

    @Test
    fun `a mark that repeats is grouping`() {
        assertParses(valid(100_000_000), "1.000.000", zloty)
        assertParses(valid(100_000_000), "1,000,000", zloty)
        assertParses(valid(1_000_000), "1,000,000", yen)
    }

    @Test
    fun `a different separator before the last mark makes it decimal`() {
        assertParses(valid(100_050), "1.000,50", zloty)
        assertParses(valid(100_050), "1,000.50", zloty)
        assertParses(valid(1_000_000), "1.000,000", dinar)
        assertParses(valid(1_000_000), "1 000,000", dinar)
        assertParses(valid(1_234_500), "1 234.5", dinar)
    }

    @Test
    fun `surrounding white space is trimmed`() {
        assertParses(valid(10_050), "  100,50 ", zloty)
        assertParses(valid(10_050), " 100,50 ", zloty)
        assertParses(valid(10_050), "\t100,50\n", zloty)
    }

    @Test
    fun `nothing typed is empty, not unreadable`() {
        assertParses(ParsedAmount.Empty, "", zloty)
        assertParses(ParsedAmount.Empty, "   ", zloty)
        assertParses(ParsedAmount.Empty, " ", zloty)
    }

    @Test
    fun `anything but ASCII digits, marks and spaces is unreadable`() {
        listOf("-100", "+100", "100zł", "1e3", "abc", "100,50-", "1_000", "١٠٠", "1\t000").forEach {
            assertParses(ParsedAmount.Unreadable, it, zloty)
        }
    }

    @Test
    fun `marks without a digit are unreadable`() {
        listOf(",", ".", ",.", ", .").forEach { assertParses(ParsedAmount.Unreadable, it, zloty) }
    }

    @Test
    fun `a decimal separator needs digits after it and nothing else`() {
        assertParses(ParsedAmount.Unreadable, "100,", zloty)
        assertParses(ParsedAmount.Unreadable, "100.", zloty)
        assertParses(ParsedAmount.Unreadable, "100, 5", zloty)
        assertParses(ParsedAmount.Unreadable, "1 000,5 0", zloty)
    }

    @Test
    fun `a decimal separator needs a whole part before it`() {
        assertParses(ParsedAmount.Unreadable, ",50", zloty)
        assertParses(ParsedAmount.Unreadable, ".5", zloty)
    }

    @Test
    fun `more decimals than the Currency holds are refused with how many it allows`() {
        assertParses(ParsedAmount.TooManyDecimals(2), "100,5050", zloty)
        assertParses(ParsedAmount.TooManyDecimals(2), "1 000,505", zloty)
        assertParses(ParsedAmount.TooManyDecimals(2), "1,0000", zloty)
        assertParses(ParsedAmount.TooManyDecimals(0), "100.5", yen)
        assertParses(ParsedAmount.TooManyDecimals(0), "100,0", yen)
        assertParses(ParsedAmount.TooManyDecimals(3), "1,0005", dinar)
    }

    @Test
    fun `a lone mark before three digits is ambiguous at three or more fraction digits`() {
        assertParses(ParsedAmount.Ambiguous, "1,000", dinar)
        assertParses(ParsedAmount.Ambiguous, "1.000", dinar)
        assertParses(ParsedAmount.Ambiguous, "1,000", unitOfAccount)
        assertParses(ParsedAmount.Ambiguous, "1.000", unitOfAccount)
    }

    @Test
    fun `under three digits, three decimals below a thousand cannot be typed unambiguously`() {
        assertParses(ParsedAmount.Ambiguous, "0,001", dinar)
        assertParses(ParsedAmount.Ambiguous, "1,234", dinar)
        assertParses(valid(1_000_001), "1 000,001", dinar)
    }

    @Test
    fun `fewer decimals than three read plainly in dinars and four-digit units`() {
        assertParses(valid(1_500), "1,5", dinar)
        assertParses(valid(1_050), "1,05", dinar)
        assertParses(valid(15_000), "1,5", unitOfAccount)
        assertParses(valid(12_345), "1,2345", unitOfAccount)
    }

    @Test
    fun `grouping other than threes after a first group of one to three is unreadable`() {
        listOf("1,00,000", "1.0000.000", "1234.567.890", "1  000", "1 00", "10 0000", ",000", "1 000.000.000").forEach {
            assertParses(ParsedAmount.Unreadable, it, zloty)
        }
    }

    @Test
    fun `a lone grouping mark after four digits is unreadable`() {
        assertParses(ParsedAmount.Unreadable, "1234,567", zloty)
    }

    @Test
    fun `zero is refused however it is written`() {
        listOf("0", "000", "0,00", "0.0", "0 000", "0,000").forEach { assertParses(ParsedAmount.Zero, it, zloty) }
        assertParses(ParsedAmount.Zero, "0", yen)
    }

    @Test
    fun `the smallest Amount is one minor unit`() {
        assertParses(valid(1), "0,01", zloty)
        assertParses(valid(1), "1", yen)
    }

    @Test
    fun `the whole part stays below ten to the twelfth`() {
        assertParses(valid(99_999_999_999_999), "999999999999,99", zloty)
        assertParses(valid(99_999_999_999_999), "999 999 999 999,99", zloty)
        assertParses(valid(999_999_999_999_999), "999 999 999 999,999", dinar)
        assertParses(valid(999_999_999_999), "999.999.999.999", yen)
        assertParses(ParsedAmount.TooLarge, "1000000000000", zloty)
        assertParses(ParsedAmount.TooLarge, "1 000 000 000 000", yen)
        assertParses(ParsedAmount.TooLarge, "1.000.000.000.000,00", zloty)
        assertParses(ParsedAmount.TooLarge, "99999999999999999999999999", zloty)
    }

    @Test
    fun `leading zeros do not count towards the bound`() {
        assertParses(valid(99_999_999_999_999), "000999999999999,99", zloty)
    }

    @Test
    fun `the largest parseable Amount times a thousand still fits a Long`() {
        val largest = (parseAmount("999 999 999 999,999", dinar) as ParsedAmount.Valid).minorUnits
        assertEquals(AMOUNT_WHOLE_LIMIT * 1000 - 1, largest)
        assertEquals(largest * 1000, Math.multiplyExact(largest, 1000L))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative fraction digits are a programming error`() {
        parseAmount("1", -1)
    }
}
