package org.hoods.pocket.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.domain.model.MonthlyBudget
import org.hoods.pocket.domain.model.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeleteMonthlyBudgetUseCaseTest {

    @Test
    fun deletesExistingBudget_returnsSuccessAndRemovesFromRepository() = runTest {
        val repository = FakeBudgetRepository()
        val useCase = DeleteMonthlyBudgetUseCase(repository)
        val targetMonth = YearMonth(2026, 9)
        val budget = MonthlyBudget(targetMonth, 250000L)

        repository.setMonthlyBudget(budget)
        assertEquals(budget, repository.getMonthlyBudget(targetMonth).first())

        val result = useCase(targetMonth)

        assertTrue(result.isSuccess)
        assertNull(repository.getMonthlyBudget(targetMonth).first())
    }

    @Test
    fun repositoryError_returnsFailure() = runTest {
        val repository = FakeBudgetRepository()
        val useCase = DeleteMonthlyBudgetUseCase(repository)
        val targetMonth = YearMonth(2026, 9)

        repository.shouldFail = true
        repository.failureMessage = "Disk full"

        val result = useCase(targetMonth)

        assertTrue(result.isFailure)
        assertEquals("Disk full", result.exceptionOrNull()?.message)
    }

    @Test
    fun deletesExistingBudget_alsoCleansUpCategoryBudgetsForMonth() = runTest {
        val repository = FakeBudgetRepository()
        val categoryRepository = FakeCategoryBudgetRepository()
        val useCase = DeleteMonthlyBudgetUseCase(repository, categoryRepository)
        val targetMonth = YearMonth(2026, 9)

        repository.setMonthlyBudget(MonthlyBudget(targetMonth, 250000L))
        categoryRepository.setCategoryBudget(
            org.hoods.pocket.domain.model.CategoryBudget(targetMonth, org.hoods.pocket.domain.model.TransactionCategory.GROCERIES, 50000L)
        )

        val result = useCase(targetMonth)

        assertTrue(result.isSuccess)
        assertNull(repository.getMonthlyBudget(targetMonth).first())
        assertTrue(categoryRepository.getCategoryBudgets(targetMonth).first().isEmpty())
    }
}
