package app.ioutracker.ui.loan

import app.ioutracker.data.CounterpartyBalance
import app.ioutracker.data.CounterpartyKind
import app.ioutracker.ui.common.ParsedAmount
import app.ioutracker.ui.common.parseAmount
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class AddLoanFormTest {
    @Test
    fun `each form the ambiguity offers reads one way only, as what it says`() {
        val forms = ambiguityForms(" 1,234 ")
        assertEquals(AmbiguityForms(asFraction = "1,2340", asWhole = "1 234"), forms)
        val typed = parseAmount("1,234", 3) as ParsedAmount.Ambiguous
        assertEquals(ParsedAmount.Valid(typed.asDecimal), parseAmount(forms.asFraction, 3))
        assertEquals(ParsedAmount.Valid(typed.asGrouped), parseAmount(forms.asWhole, 3))
    }

    @Test
    fun `the example parses in every offered scale and both languages`() {
        for (locale in listOf(Locale.ENGLISH, Locale.forLanguageTag("pl"))) {
            for (digits in 0..3) {
                val example = amountExample(digits, locale)
                assertEquals("$example at $digits", ParsedAmount.Valid::class, parseAmount(example, digits)::class)
            }
        }
        assertEquals("1250,50", amountExample(2, Locale.forLanguageTag("pl")))
        assertEquals("1250", amountExample(0, Locale.ENGLISH))
    }

    private fun counterparty(name: String) = CounterpartyBalance(name, name, CounterpartyKind.PERSON, 0)

    private val existing = listOf(counterparty("Ola"), counterparty("Olek"), counterparty("Łukasz"))

    @Test
    fun `suggestions contain the typed text ignoring case, and stop once it names exactly one`() {
        assertEquals(listOf("Ola", "Olek"), suggestionsFor("ol", existing).map { it.name })
        assertEquals(listOf("Łukasz"), suggestionsFor("łuk", existing).map { it.name })
        assertEquals(emptyList<CounterpartyBalance>(), suggestionsFor(" ŁUKASZ ", existing))
        assertEquals(emptyList<CounterpartyBalance>(), suggestionsFor("  ", existing))
    }
}
