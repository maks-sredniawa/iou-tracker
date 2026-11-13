package app.ioutracker.ui.currency

import app.ioutracker.data.RescalePlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class CurrencyOptionsTest {
    private val codes = listOf("USD", "JPY", "EUR", "PLN", "KWD")

    @Test
    fun `the current and the region's currency come first, then the rest by localised name`() {
        val options = currencyOptions(codes, pinned = listOf("PLN", "KWD"), query = "", locale = Locale.ENGLISH)
        assertEquals(listOf("PLN", "KWD", "EUR", "JPY", "USD"), options.map { it.code })
    }

    @Test
    fun `a currency pinned twice is listed once`() {
        val options = currencyOptions(codes, pinned = listOf("PLN", "PLN"), query = "", locale = Locale.ENGLISH)
        assertEquals(codes.size, options.size)
    }

    @Test
    fun `a held code outside the offered list is still shown`() {
        val options = currencyOptions(codes, pinned = listOf("DEM"), query = "", locale = Locale.ENGLISH)
        assertEquals("DEM", options.first().code)
    }

    @Test
    fun `search matches the localised name or the code, ignoring case`() {
        val polish = Locale.forLanguageTag("pl")
        assertEquals(listOf("PLN"), currencyOptions(codes, emptyList(), "ZŁOTY", polish).map { it.code })
        assertEquals(listOf("JPY"), currencyOptions(codes, emptyList(), "jpy", polish).map { it.code })
    }

    @Test
    fun `an X code that names no currency is not offered, even where the platform gives it two digits`() {
        val notCurrencies =
            listOf("XAU", "XAG", "XPT", "XPD", "XDR", "XSU", "XUA", "XBA", "XBB", "XBC", "XBD", "XTS", "XXX", "XFU")
        for (code in notCurrencies) assertFalse(code, isOfferedCurrency(code, 2))
    }

    @Test
    fun `the X codes that are currencies in use are offered`() {
        for ((code, digits) in listOf("XAF" to 0, "XOF" to 0, "XPF" to 0, "XCD" to 2, "XCG" to 2)) {
            assertTrue(code, isOfferedCurrency(code, digits))
        }
        assertTrue(isOfferedCurrency("PLN", 2))
        assertFalse(isOfferedCurrency("CLF", 4))
    }

    @Test
    fun `the warning's after is the same number at the new scale`() {
        assertEquals(100_500L, rescaled(10_050, RescalePlan.Multiply(10)))
        assertEquals(150L, rescaled(15_000, RescalePlan.Divide(100)))
        assertEquals(10_050L, rescaled(10_050, RescalePlan.Relabel))
    }
}
