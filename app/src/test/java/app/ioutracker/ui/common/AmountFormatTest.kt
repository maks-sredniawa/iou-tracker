package app.ioutracker.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Currency
import java.util.Locale

class AmountFormatTest {
    @Test
    fun `a code the platform cannot name is shown by its code, at its own digits`() {
        val english = formatInCurrency(10_050, "VES", 2, currency = null, locale = Locale.US)
        assertTrue(english, english.contains("VES") && english.contains("100.50"))
        val polish = formatInCurrency(100_000, "VES", 2, currency = null, locale = Locale.forLanguageTag("pl-PL"))
        assertTrue(polish, polish.contains("VES") && polish.contains(",00"))
    }

    @Test
    fun `a code the platform names keeps its own symbol`() {
        assertEquals(
            formatAmount(10_050, "PLN", Locale.US),
            formatInCurrency(10_050, "PLN", 2, Currency.getInstance("PLN"), Locale.US),
        )
        assertTrue(formatAmount(1_500, "JPY", Locale.US).contains("1,500"))
    }

    @Test
    fun `a code nobody knows shows the bare count of minor units`() {
        assertEquals("10,050", formatAmount(10_050, "XQQ", Locale.US))
    }
}
