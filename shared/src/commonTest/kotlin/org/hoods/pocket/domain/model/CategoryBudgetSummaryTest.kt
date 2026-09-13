package org.hoods.pocket.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class CategoryBudgetSummaryTest {

    @Test
    fun constructor_validExpenseCategory_createsSuccessfully() {
        val summary = CategoryBudgetSummary(
            category = TransactionCategory.GROCERIES,
            limitAmount = 60000L,
            spentAmount = 45000L,
            remainingAmount = 15000L,
            progress = 0.75f,
            status = BudgetHealthStatus.ON_TRACK
        )

        assertNotNull(summary)
        assertEquals(TransactionCategory.GROCERIES, summary.category)
        assertEquals(60000L, summary.limitAmount)
        assertEquals(45000L, summary.spentAmount)
        assertEquals(15000L, summary.remainingAmount)
        assertEquals(0.75f, summary.progress)
        assertEquals(BudgetHealthStatus.ON_TRACK, summary.status)
    }

    @Test
    fun constructor_rejectsIncomeCategory() {
        val exception = assertFailsWith<IllegalArgumentException> {
            CategoryBudgetSummary(
                category = TransactionCategory.SALARY,
                limitAmount = 100000L,
                spentAmount = 0L,
                remainingAmount = 100000L,
                progress = 0f,
                status = BudgetHealthStatus.ON_TRACK
            )
        }
        assertEquals("CategoryBudgetSummary only applies to expense categories", exception.message)
    }

    @Test
    fun constructor_rejectsUncategorized() {
        val exception = assertFailsWith<IllegalArgumentException> {
            CategoryBudgetSummary(
                category = TransactionCategory.UNCATEGORIZED,
                limitAmount = 50000L,
                spentAmount = 0L,
                remainingAmount = 50000L,
                progress = 0f,
                status = BudgetHealthStatus.ON_TRACK
            )
        }
        assertEquals("CategoryBudgetSummary only applies to expense categories", exception.message)
    }

    @Test
    fun constructor_rejectsZeroOrNegativeLimit() {
        val zeroException = assertFailsWith<IllegalArgumentException> {
            CategoryBudgetSummary(
                category = TransactionCategory.GROCERIES,
                limitAmount = 0L,
                spentAmount = 0L,
                remainingAmount = 0L,
                progress = 0f,
                status = BudgetHealthStatus.ON_TRACK
            )
        }
        assertEquals("Category budget limit must be greater than zero", zeroException.message)

        val negativeException = assertFailsWith<IllegalArgumentException> {
            CategoryBudgetSummary(
                category = TransactionCategory.GROCERIES,
                limitAmount = -100L,
                spentAmount = 0L,
                remainingAmount = -100L,
                progress = 0f,
                status = BudgetHealthStatus.ON_TRACK
            )
        }
        assertEquals("Category budget limit must be greater than zero", negativeException.message)
    }
}
