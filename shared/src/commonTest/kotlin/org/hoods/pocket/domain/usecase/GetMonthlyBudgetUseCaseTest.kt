package org.hoods.pocket.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.domain.model.MonthlyBudget
import org.hoods.pocket.domain.model.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetMonthlyBudgetUseCaseTest {

    @Test
    fun returnsBudget_whenBudgetExistsForTargetMonth() = runTest {
        val repository = FakeBudgetRepository()
        val useCase = GetMonthlyBudgetUseCase(repository)
        val targetMonth = YearMonth(2026, 9)
        val budget = MonthlyBudget(targetMonth, 250000L)

        repository.setMonthlyBudget(budget)

        val result = useCase(targetMonth).first()
        assertEquals(budget, result)
    }

    @Test
    fun returnsNull_whenNoBudgetExistsForTargetMonth() = runTest {
        val repository = FakeBudgetRepository()
        val useCase = GetMonthlyBudgetUseCase(repository)
        val targetMonth = YearMonth(2026, 9)

        val result = useCase(targetMonth).first()
        assertNull(result)
    }
}
