package app.ioutracker.ui.currency

import app.ioutracker.data.RescalePlan
import app.ioutracker.data.StoredValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CurrencyChangePlanTest {
    private val unresolvable = "XQQ"
    private val values = listOf(StoredValue("ola-1", 10_050))

    @Test
    fun `no currency yet relabels without reading a stored value`() {
        val plan = planCurrencyChange(null, newDigits = 0) { error("nothing is stored before a currency") }
        assertEquals(RescalePlan.Relabel, plan)
    }

    @Test
    fun `a stored code the platform cannot resolve is refused, not relabelled`() {
        assertNull(fractionDigitsOf(unresolvable))
        assertEquals(RescalePlan.Refused(emptyList()), planCurrencyChange(unresolvable, newDigits = 0) { values })
    }

    @Test
    fun `a stored code the platform resolves is planned by its digits`() {
        assertEquals(RescalePlan.Refused(listOf("ola-1")), planCurrencyChange("PLN", newDigits = 0) { values })
        assertEquals(RescalePlan.Multiply(10), planCurrencyChange("PLN", newDigits = 3) { values })
    }

    @Test
    fun `the refusal screen cannot judge any value of an unresolvable stored code`() {
        assertNull(rescaleDivisor(unresolvable, "JPY"))
    }

    @Test
    fun `the refusal screen divides by the digits lost, or blocks nothing`() {
        assertEquals(100L, rescaleDivisor("PLN", "JPY"))
        assertEquals(1L, rescaleDivisor("JPY", "PLN"))
        assertEquals(1L, rescaleDivisor(null, "JPY"))
    }
}
