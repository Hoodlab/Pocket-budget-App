package org.hoods.pocket.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.domain.model.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SetMonthlyBudgetUseCaseTest {

    @Test
    fun validAmount_savesMonthlyBudget() = runTest {
        val budgetRepository = FakeBudgetRepository()
        val useCase = SetMonthlyBudgetUseCase(budgetRepository)
        val targetMonth = YearMonth(2026, 9)

        val result = useCase(targetMonth, 120000L)

        assertTrue(result.isSuccess)
        val saved = budgetRepository.getMonthlyBudget(targetMonth).first()
        assertEquals(targetMonth, saved?.yearMonth)
        assertEquals(120000L, saved?.limitAmount)
    }

    @Test
    fun zeroAmount_returnsFailure() = runTest {
        val budgetRepository = FakeBudgetRepository()
        val useCase = SetMonthlyBudgetUseCase(budgetRepository)
        val targetMonth = YearMonth(2026, 9)

        val result = useCase(targetMonth, 0L)

        assertTrue(result.isFailure)
        val saved = budgetRepository.getMonthlyBudget(targetMonth).first()
        assertEquals(null, saved)
    }

    @Test
    fun negativeAmount_returnsFailure() = runTest {
        val budgetRepository = FakeBudgetRepository()
        val useCase = SetMonthlyBudgetUseCase(budgetRepository)
        val targetMonth = YearMonth(2026, 9)

        val result = useCase(targetMonth, -5000L)

        assertTrue(result.isFailure)
        val saved = budgetRepository.getMonthlyBudget(targetMonth).first()
        assertEquals(null, saved)
    }
}
