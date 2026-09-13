package org.hoods.pocket.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.domain.model.CategoryBudget
import org.hoods.pocket.domain.model.TransactionCategory
import org.hoods.pocket.domain.model.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetCategoryBudgetsUseCaseTest {

    private val categoryBudgetRepository = FakeCategoryBudgetRepository()
    private val useCase = GetCategoryBudgetsUseCase(categoryBudgetRepository)
    private val august2026 = YearMonth(2026, 8)
    private val september2026 = YearMonth(2026, 9)

    @Test
    fun returnsEmptyList_whenNoCategoryBudgetsSet() = runTest {
        val result = useCase(august2026).first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun returnsBudgetsForTargetMonth_withMonthIsolation() = runTest {
        val augGroceries = CategoryBudget(august2026, TransactionCategory.GROCERIES, 50000L)
        val augDining = CategoryBudget(august2026, TransactionCategory.DINING, 30000L)
        val sepGroceries = CategoryBudget(september2026, TransactionCategory.GROCERIES, 60000L)

        categoryBudgetRepository.setCategoryBudget(augGroceries)
        categoryBudgetRepository.setCategoryBudget(augDining)
        categoryBudgetRepository.setCategoryBudget(sepGroceries)

        val augResults = useCase(august2026).first()
        assertEquals(2, augResults.size)
        assertTrue(augResults.any { it.category == TransactionCategory.GROCERIES && it.limitAmount == 50000L })
        assertTrue(augResults.any { it.category == TransactionCategory.DINING && it.limitAmount == 30000L })

        val sepResults = useCase(september2026).first()
        assertEquals(1, sepResults.size)
        assertEquals(TransactionCategory.GROCERIES, sepResults[0].category)
        assertEquals(60000L, sepResults[0].limitAmount)
    }
}
