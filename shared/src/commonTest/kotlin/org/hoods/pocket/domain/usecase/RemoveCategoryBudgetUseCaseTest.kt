package org.hoods.pocket.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.domain.model.CategoryBudget
import org.hoods.pocket.domain.model.TransactionCategory
import org.hoods.pocket.domain.model.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RemoveCategoryBudgetUseCaseTest {

    private val categoryBudgetRepository = FakeCategoryBudgetRepository()
    private val useCase = RemoveCategoryBudgetUseCase(categoryBudgetRepository)
    private val targetMonth = YearMonth(2026, 8)

    @Test
    fun removesTargetCategoryAllocation_preservesOtherCategories() = runTest {
        categoryBudgetRepository.setCategoryBudget(
            CategoryBudget(targetMonth, TransactionCategory.GROCERIES, 50000L)
        )
        categoryBudgetRepository.setCategoryBudget(
            CategoryBudget(targetMonth, TransactionCategory.DINING, 30000L)
        )

        val result = useCase(targetMonth, TransactionCategory.DINING)
        assertTrue(result.isSuccess)

        val remaining = categoryBudgetRepository.getCategoryBudgets(targetMonth).first()
        assertEquals(1, remaining.size)
        assertEquals(TransactionCategory.GROCERIES, remaining[0].category)
    }
}
