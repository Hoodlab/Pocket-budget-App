package org.hoods.pocket.presentation.budget.set

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.usecase.FakeBudgetRepository
import org.hoods.pocket.domain.usecase.SetMonthlyBudgetUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SetBudgetViewModelTest {

    @Test
    fun `saveBudget with valid decimal amount converts to cents and emits SaveSuccess`() = runTest {
        val repository = FakeBudgetRepository()
        val useCase = SetMonthlyBudgetUseCase(repository)
        val targetMonth = YearMonth(2026, 8)

        val viewModel = SetBudgetViewModel(
            setMonthlyBudgetUseCase = useCase,
            yearMonth = targetMonth,
            coroutineScope = backgroundScope
        )

        viewModel.onAmountChanged("1200.50")
        assertEquals("1200.50", viewModel.uiState.value.amountText)

        viewModel.saveBudget()

        val event = viewModel.events.first()
        assertEquals(SetBudgetEvent.SaveSuccess, event)

        val saved = repository.getMonthlyBudget(targetMonth).first()
        assertNotNull(saved)
        assertEquals(120050L, saved.limitAmount)
        assertEquals(targetMonth, saved.yearMonth)
    }

    @Test
    fun `saveBudget with blank amount displays error message`() = runTest {
        val repository = FakeBudgetRepository()
        val useCase = SetMonthlyBudgetUseCase(repository)
        val targetMonth = YearMonth(2026, 8)

        val viewModel = SetBudgetViewModel(
            setMonthlyBudgetUseCase = useCase,
            yearMonth = targetMonth,
            coroutineScope = backgroundScope
        )

        viewModel.onAmountChanged("   ")
        viewModel.saveBudget()

        assertEquals("Budget amount must not be blank", viewModel.uiState.value.errorMessage)
        val saved = repository.getMonthlyBudget(targetMonth).first()
        assertNull(saved)
    }

    @Test
    fun `saveBudget with zero amount displays validation error`() = runTest {
        val repository = FakeBudgetRepository()
        val useCase = SetMonthlyBudgetUseCase(repository)
        val targetMonth = YearMonth(2026, 8)

        val viewModel = SetBudgetViewModel(
            setMonthlyBudgetUseCase = useCase,
            yearMonth = targetMonth,
            coroutineScope = backgroundScope
        )

        viewModel.onAmountChanged("0.00")
        viewModel.saveBudget()

        assertEquals("Please enter a valid amount greater than zero", viewModel.uiState.value.errorMessage)
        val saved = repository.getMonthlyBudget(targetMonth).first()
        assertNull(saved)
    }

    @Test
    fun `onAmountChanged clears previous error`() = runTest {
        val repository = FakeBudgetRepository()
        val useCase = SetMonthlyBudgetUseCase(repository)
        val targetMonth = YearMonth(2026, 8)

        val viewModel = SetBudgetViewModel(
            setMonthlyBudgetUseCase = useCase,
            yearMonth = targetMonth,
            coroutineScope = backgroundScope
        )

        viewModel.saveBudget()
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.onAmountChanged("500")
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals("500", viewModel.uiState.value.amountText)
    }
}
