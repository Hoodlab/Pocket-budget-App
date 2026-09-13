package org.hoods.pocket.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.domain.model.CategoryBudget
import org.hoods.pocket.domain.model.MonthlyBudget
import org.hoods.pocket.domain.model.TransactionCategory
import org.hoods.pocket.domain.model.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SetCategoryBudgetUseCaseTest {

    private val budgetRepository = FakeBudgetRepository()
    private val categoryBudgetRepository = FakeCategoryBudgetRepository()
    private val useCase = SetCategoryBudgetUseCase(budgetRepository, categoryBudgetRepository)
    private val targetMonth = YearMonth(2026, 8)

    @Test
    fun validAllocation_whenMonthlyBudgetExists_savesCategoryBudget() = runTest {
        budgetRepository.setMonthlyBudget(MonthlyBudget(targetMonth, 200000L)) // $2,000.00

        val result = useCase(
            yearMonth = targetMonth,
            category = TransactionCategory.GROCERIES,
            amountCents = 50000L // $500.00
        )

        assertTrue(result.isSuccess)
        val budget = result.getOrNull()
        assertEquals(targetMonth, budget?.yearMonth)
        assertEquals(TransactionCategory.GROCERIES, budget?.category)
        assertEquals(50000L, budget?.limitAmount)

        val persisted = categoryBudgetRepository.getCategoryBudgets(targetMonth).first()
        assertEquals(1, persisted.size)
        assertEquals(50000L, persisted[0].limitAmount)
    }

    @Test
    fun noMonthlyBudget_returnsFailure() = runTest {
        val result = useCase(
            yearMonth = targetMonth,
            category = TransactionCategory.GROCERIES,
            amountCents = 50000L
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("monthly budget", ignoreCase = true) == true)
    }

    @Test
    fun ineligibleIncomeCategory_returnsFailure() = runTest {
        budgetRepository.setMonthlyBudget(MonthlyBudget(targetMonth, 200000L))

        val result = useCase(
            yearMonth = targetMonth,
            category = TransactionCategory.SALARY,
            amountCents = 50000L
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun zeroOrNegativeAmount_returnsFailure() = runTest {
        budgetRepository.setMonthlyBudget(MonthlyBudget(targetMonth, 200000L))

        val zeroResult = useCase(targetMonth, TransactionCategory.GROCERIES, 0L)
        assertTrue(zeroResult.isFailure)

        val negResult = useCase(targetMonth, TransactionCategory.GROCERIES, -100L)
        assertTrue(negResult.isFailure)
    }

    @Test
    fun overAllocationExceedingMonthlyBudget_returnsFailure() = runTest {
        budgetRepository.setMonthlyBudget(MonthlyBudget(targetMonth, 100000L)) // $1,000.00 total
        categoryBudgetRepository.setCategoryBudget(
            CategoryBudget(targetMonth, TransactionCategory.GROCERIES, 70000L) // $700.00 already used
        )

        // Attempt to allocate $400.00 to DINING ($700 + $400 = $1,100 > $1,000)
        val result = useCase(
            yearMonth = targetMonth,
            category = TransactionCategory.DINING,
            amountCents = 40000L
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("exceed", ignoreCase = true) == true)
    }

    @Test
    fun updateExistingCategoryAllocation_evaluatesNewTotalWithoutDuplicates() = runTest {
        budgetRepository.setMonthlyBudget(MonthlyBudget(targetMonth, 100000L)) // $1,000.00 total
        categoryBudgetRepository.setCategoryBudget(
            CategoryBudget(targetMonth, TransactionCategory.GROCERIES, 60000L) // $600.00
        )
        categoryBudgetRepository.setCategoryBudget(
            CategoryBudget(targetMonth, TransactionCategory.DINING, 30000L) // $300.00
        )
        // Total currently: $900.00

        // Updating GROCERIES from $600 to $700 (New total $700 + $300 = $1,000 <= $1,000)
        val updateSuccess = useCase(
            yearMonth = targetMonth,
            category = TransactionCategory.GROCERIES,
            amountCents = 70000L
        )
        assertTrue(updateSuccess.isSuccess)

        val persisted = categoryBudgetRepository.getCategoryBudgets(targetMonth).first()
        assertEquals(2, persisted.size)
        val updatedGroceries = persisted.find { it.category == TransactionCategory.GROCERIES }
        assertEquals(70000L, updatedGroceries?.limitAmount)
    }
}
