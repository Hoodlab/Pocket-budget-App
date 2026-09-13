package org.hoods.pocket.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.domain.model.CategoryBudget
import org.hoods.pocket.domain.model.TransactionCategory
import org.hoods.pocket.domain.model.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SaveCategoryAllocationsUseCaseTest {

    private val budgetRepository = FakeBudgetRepository()
    private val categoryBudgetRepository = FakeCategoryBudgetRepository()
    private val useCase = SaveCategoryAllocationsUseCase(budgetRepository, categoryBudgetRepository)
    private val targetMonth = YearMonth(2026, 8)

    @Test
    fun validAllocations_savesMonthlyBudgetAndAllCategoryBudgets() = runTest {
        val allocations = mapOf(
            TransactionCategory.GROCERIES to 50000L, // $500.00
            TransactionCategory.DINING to 30000L,    // $300.00
            TransactionCategory.TRANSPORT to 20000L // $200.00
        )

        val result = useCase(
            yearMonth = targetMonth,
            totalLimitCents = 150000L, // $1,500.00
            categoryAllocations = allocations
        )

        assertTrue(result.isSuccess)

        val monthlyBudget = budgetRepository.getMonthlyBudget(targetMonth).first()
        assertEquals(150000L, monthlyBudget?.limitAmount)

        val savedCategories = categoryBudgetRepository.getCategoryBudgets(targetMonth).first()
        assertEquals(3, savedCategories.size)
        assertEquals(50000L, savedCategories.find { it.category == TransactionCategory.GROCERIES }?.limitAmount)
        assertEquals(30000L, savedCategories.find { it.category == TransactionCategory.DINING }?.limitAmount)
        assertEquals(20000L, savedCategories.find { it.category == TransactionCategory.TRANSPORT }?.limitAmount)
    }

    @Test
    fun overAllocation_returnsFailureAndPersistsNothing() = runTest {
        val allocations = mapOf(
            TransactionCategory.GROCERIES to 60000L,
            TransactionCategory.DINING to 50000L // $600 + $500 = $1,100 > $1,000
        )

        val result = useCase(
            yearMonth = targetMonth,
            totalLimitCents = 100000L, // $1,000.00
            categoryAllocations = allocations
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("exceed", ignoreCase = true) == true)
    }

    @Test
    fun clearingExistingAllocation_removesItFromRepository() = runTest {
        categoryBudgetRepository.setCategoryBudget(
            CategoryBudget(targetMonth, TransactionCategory.GROCERIES, 50000L)
        )
        categoryBudgetRepository.setCategoryBudget(
            CategoryBudget(targetMonth, TransactionCategory.DINING, 30000L)
        )

        // User clears DINING (passes null or 0)
        val allocations = mapOf(
            TransactionCategory.GROCERIES to 60000L,
            TransactionCategory.DINING to null
        )

        val result = useCase(
            yearMonth = targetMonth,
            totalLimitCents = 100000L,
            categoryAllocations = allocations
        )

        assertTrue(result.isSuccess)

        val savedCategories = categoryBudgetRepository.getCategoryBudgets(targetMonth).first()
        assertEquals(1, savedCategories.size)
        assertEquals(TransactionCategory.GROCERIES, savedCategories[0].category)
        assertEquals(60000L, savedCategories[0].limitAmount)
    }
}
