package app.ioutracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class CurrencyRegionTest {
    private fun locales(vararg tags: String) = tags.map(Locale::forLanguageTag)

    @Test
    fun `the first locale's region decides`() {
        assertEquals("PLN", currencyCodeForRegions(locales("pl-PL", "en-US")))
    }

    @Test
    fun `a locale with no region is passed over`() {
        assertEquals("JPY", currencyCodeForRegions(locales("en", "ja-JP")))
    }

    @Test
    fun `no region naming a currency leaves it empty`() {
        assertNull(currencyCodeForRegions(locales("en", "es-419", "en-AQ")))
        assertNull(currencyCodeForRegions(emptyList()))
    }

    @Test
    fun `a withdrawn code an old platform still names is stored as its successor`() {
        val api26 =
            mapOf(
                "CW" to "ANG",
                "BG" to "BGN",
                "CU" to "CUC",
                "HR" to "HRK",
                "MR" to "MRO",
                "SL" to "SLL",
                "ST" to "STD",
                "VE" to "VEF",
                "ZW" to "ZWL",
            )
        val expected =
            mapOf(
                "CW" to "XCG",
                "BG" to "EUR",
                "CU" to "CUP",
                "HR" to "EUR",
                "MR" to "MRU",
                "SL" to "SLE",
                "ST" to "STN",
                "VE" to "VES",
                "ZW" to "ZWG",
            )
        for ((region, successor) in expected) {
            assertEquals(region, successor, currencyCodeForRegions(locales("und-$region")) { api26[it.country] })
        }
    }

    @Test
    fun `a Bulgarian region starts in EUR, not the withdrawn BGN`() {
        val api26 = mapOf("BG" to "BGN", "US" to "USD")
        assertEquals("EUR", currencyCodeForRegions(locales("bg-BG", "en-US")) { api26[it.country] })
    }

    @Test
    fun `a current code passes through unchanged`() {
        assertEquals("PLN", currencyCodeForRegions(locales("pl-PL")) { "PLN" })
    }
}
