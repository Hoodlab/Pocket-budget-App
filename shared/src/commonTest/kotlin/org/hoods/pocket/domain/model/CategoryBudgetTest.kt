package org.hoods.pocket.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CategoryBudgetTest {

    private val targetMonth = YearMonth(2026, 8)

    @Test
    fun validExpenseCategory_createsCategoryBudget() {
        val budget = CategoryBudget(
            yearMonth = targetMonth,
            category = TransactionCategory.GROCERIES,
            limitAmount = 50000L
        )

        assertEquals(targetMonth, budget.yearMonth)
        assertEquals(TransactionCategory.GROCERIES, budget.category)
        assertEquals(50000L, budget.limitAmount)
    }

    @Test
    fun incomeCategory_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            CategoryBudget(
                yearMonth = targetMonth,
                category = TransactionCategory.SALARY,
                limitAmount = 50000L
            )
        }
    }

    @Test
    fun uncategorized_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            CategoryBudget(
                yearMonth = targetMonth,
                category = TransactionCategory.UNCATEGORIZED,
                limitAmount = 50000L
            )
        }
    }

    @Test
    fun otherSharedCategory_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            CategoryBudget(
                yearMonth = targetMonth,
                category = TransactionCategory.OTHER,
                limitAmount = 50000L
            )
        }
    }

    @Test
    fun zeroOrNegativeLimit_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            CategoryBudget(
                yearMonth = targetMonth,
                category = TransactionCategory.DINING,
                limitAmount = 0L
            )
        }

        assertFailsWith<IllegalArgumentException> {
            CategoryBudget(
                yearMonth = targetMonth,
                category = TransactionCategory.DINING,
                limitAmount = -1000L
            )
        }
    }
}
