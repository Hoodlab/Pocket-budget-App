package org.hoods.pocket.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class CashFlowSummaryTest {

    private val testMonth = YearMonth(2026, 8)

    @Test
    fun constructor_validValues_computesNetBalanceCorrectly() {
        val summary = CashFlowSummary(
            yearMonth = testMonth,
            income = 450000L,
            expense = 258000L
        )

        assertNotNull(summary)
        assertEquals(testMonth, summary.yearMonth)
        assertEquals(450000L, summary.income)
        assertEquals(258000L, summary.expense)
        assertEquals(192000L, summary.netBalance)
    }

    @Test
    fun constructor_explicitNegativeNetBalance_whenExpenseExceedsIncome() {
        val summary = CashFlowSummary(
            yearMonth = testMonth,
            income = 200000L,
            expense = 350000L
        )

        assertEquals(-150000L, summary.netBalance)
    }

    @Test
    fun constructor_zeroValues_allowedWithZeroNetBalance() {
        val summary = CashFlowSummary(
            yearMonth = testMonth,
            income = 0L,
            expense = 0L
        )

        assertEquals(0L, summary.income)
        assertEquals(0L, summary.expense)
        assertEquals(0L, summary.netBalance)
    }

    @Test
    fun constructor_rejectsNegativeIncome() {
        val exception = assertFailsWith<IllegalArgumentException> {
            CashFlowSummary(
                yearMonth = testMonth,
                income = -100L,
                expense = 5000L
            )
        }
        assertEquals("Income must not be negative", exception.message)
    }

    @Test
    fun constructor_rejectsNegativeExpense() {
        val exception = assertFailsWith<IllegalArgumentException> {
            CashFlowSummary(
                yearMonth = testMonth,
                income = 10000L,
                expense = -50L
            )
        }
        assertEquals("Expense must not be negative", exception.message)
    }
}
