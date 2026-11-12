package app.ioutracker.ui.loan

import app.ioutracker.data.CounterpartyBalance
import app.ioutracker.data.CounterpartyKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CounterpartyMatchTest {
    private val ola = CounterpartyBalance("ola", "Ola", CounterpartyKind.PERSON, 0)
    private val lukasz = CounterpartyBalance("lukasz", "Łukasz", CounterpartyKind.PERSON, 0)
    private val savings = CounterpartyBalance("savings", "Joint savings", CounterpartyKind.POT, 0)
    private val existing = listOf(ola, lukasz, savings)

    @Test
    fun `surrounding white space is trimmed`() {
        assertEquals(ola, matchCounterparty("  Ola\t", existing))
        assertEquals(ola, matchCounterparty(" Ola ", existing))
    }

    @Test
    fun `inner runs of white space collapse to one space`() {
        assertEquals(savings, matchCounterparty("Joint   savings", existing))
        assertEquals(savings, matchCounterparty("Joint \tsavings", existing))
    }

    @Test
    fun `case is ignored, beyond ASCII too`() {
        assertEquals(ola, matchCounterparty("OLA", existing))
        assertEquals(ola, matchCounterparty("ola", existing))
        assertEquals(lukasz, matchCounterparty("łukasz", existing))
        assertEquals(lukasz, matchCounterparty("ŁUKASZ", existing))
        assertEquals(savings, matchCounterparty("JOINT SAVINGS", existing))
    }

    @Test
    fun `diacritics are not ignored`() {
        assertNull(matchCounterparty("Lukasz", existing))
    }

    @Test
    fun `a stored name is matched after the same tidying`() {
        val untidy = CounterpartyBalance("kasia", " Kasia  B. ", CounterpartyKind.PERSON, 0)
        assertEquals(untidy, matchCounterparty("kasia b.", listOf(untidy)))
    }

    @Test
    fun `a new name matches nothing`() {
        assertNull(matchCounterparty("Ola K.", existing))
        assertNull(matchCounterparty("Ola", emptyList()))
    }

    @Test
    fun `nothing typed matches nothing`() {
        assertNull(matchCounterparty("", existing))
        assertNull(matchCounterparty("  \t ", existing))
    }

    @Test
    fun `the stored name is the typed text, trimmed and collapsed, its case kept`() {
        assertEquals("Ola K.", storedCounterpartyName("  Ola   K.  "))
        assertEquals("łukasz", storedCounterpartyName("łukasz"))
        assertEquals("", storedCounterpartyName(" \n "))
    }
}
