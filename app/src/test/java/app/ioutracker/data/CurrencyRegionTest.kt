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
}
