package org.hoods.pocket.domain.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MoneyParserTest {

    @Test
    fun parseAmountToCents_validStandardAmounts() {
        assertEquals(4550L, MoneyParser.parseAmountToCents("45.50"))
        assertEquals(4550L, MoneyParser.parseAmountToCents("45.5"))
        assertEquals(4500L, MoneyParser.parseAmountToCents("45"))
        assertEquals(99L, MoneyParser.parseAmountToCents("0.99"))
        assertEquals(5L, MoneyParser.parseAmountToCents("0.05"))
        assertEquals(300000L, MoneyParser.parseAmountToCents("3000.00"))
        assertEquals(4550L, MoneyParser.parseAmountToCents("$45.50"))
        assertEquals(4550L, MoneyParser.parseAmountToCents("  $ 45.50  "))
    }

    @Test
    fun parseAmountToCents_rejectsInvalidOrZeroAmounts() {
        assertNull(MoneyParser.parseAmountToCents(""))
        assertNull(MoneyParser.parseAmountToCents("   "))
        assertNull(MoneyParser.parseAmountToCents("0"))
        assertNull(MoneyParser.parseAmountToCents("0.00"))
        assertNull(MoneyParser.parseAmountToCents("-5.00"))
        assertNull(MoneyParser.parseAmountToCents("abc"))
        assertNull(MoneyParser.parseAmountToCents("12.345"))
        assertNull(MoneyParser.parseAmountToCents("12.3.4"))
        assertNull(MoneyParser.parseAmountToCents("$$50"))
    }

    @Test
    fun formatCentsToAmount_formatsCorrectly() {
        assertEquals("45.50", MoneyParser.formatCentsToAmount(4550L))
        assertEquals("0.05", MoneyParser.formatCentsToAmount(5L))
        assertEquals("0.99", MoneyParser.formatCentsToAmount(99L))
        assertEquals("3000.00", MoneyParser.formatCentsToAmount(300000L))
        assertEquals("0.00", MoneyParser.formatCentsToAmount(0L))
    }
}
