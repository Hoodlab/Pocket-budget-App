package org.hoods.pocket.presentation.budget.set

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.domain.model.MonthlyBudget
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.usecase.DeleteMonthlyBudgetUseCase
import org.hoods.pocket.domain.usecase.FakeBudgetRepository
import org.hoods.pocket.domain.usecase.GetMonthlyBudgetUseCase
import org.hoods.pocket.domain.usecase.SetMonthlyBudgetUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SetBudgetViewModelTest {

    @Test
    fun `saveBudget with valid decimal amount converts to cents and emits SaveSuccess`() = runTest {
        val repository = FakeBudgetRepository()
        val setUseCase = SetMonthlyBudgetUseCase(repository)
        val getUseCase = GetMonthlyBudgetUseCase(repository)
        val deleteUseCase = DeleteMonthlyBudgetUseCase(repository)
        val targetMonth = YearMonth(2026, 8)

        val viewModel = SetBudgetViewModel(
            setMonthlyBudgetUseCase = setUseCase,
            getMonthlyBudgetUseCase = getUseCase,
            deleteMonthlyBudgetUseCase = deleteUseCase,
            yearMonth = targetMonth,
            coroutineScope = this
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
    fun `existingBudget_loadsInitialAmountAndSetsExistingFlag`() = runTest {
        val repository = FakeBudgetRepository()
        val targetMonth = YearMonth(2026, 8)
        repository.setMonthlyBudget(MonthlyBudget(targetMonth, 350000L)) // $3,500.00

        val setUseCase = SetMonthlyBudgetUseCase(repository)
        val getUseCase = GetMonthlyBudgetUseCase(repository)
        val deleteUseCase = DeleteMonthlyBudgetUseCase(repository)

        val viewModel = SetBudgetViewModel(
            setMonthlyBudgetUseCase = setUseCase,
            getMonthlyBudgetUseCase = getUseCase,
            deleteMonthlyBudgetUseCase = deleteUseCase,
            yearMonth = targetMonth,
            coroutineScope = this
        )
        advanceUntilIdle()

        assertEquals("3500.00", viewModel.uiState.value.amountText)
        assertTrue(viewModel.uiState.value.isExistingBudget)
        assertTrue(viewModel.uiState.value.hasExistingBudget)
    }

    @Test
    fun `updateBudget_savesChangedLimitAndEmitsSaveSuccess`() = runTest {
        val repository = FakeBudgetRepository()
        val targetMonth = YearMonth(2026, 8)
        repository.setMonthlyBudget(MonthlyBudget(targetMonth, 350000L))

        val setUseCase = SetMonthlyBudgetUseCase(repository)
        val getUseCase = GetMonthlyBudgetUseCase(repository)
        val deleteUseCase = DeleteMonthlyBudgetUseCase(repository)

        val viewModel = SetBudgetViewModel(
            setMonthlyBudgetUseCase = setUseCase,
            getMonthlyBudgetUseCase = getUseCase,
            deleteMonthlyBudgetUseCase = deleteUseCase,
            yearMonth = targetMonth,
            coroutineScope = this
        )
        advanceUntilIdle()

        viewModel.onAmountChanged("4200.00")
        viewModel.saveBudget()

        val event = viewModel.events.first()
        assertEquals(SetBudgetEvent.SaveSuccess, event)

        val updated = repository.getMonthlyBudget(targetMonth).first()
        assertNotNull(updated)
        assertEquals(420000L, updated.limitAmount)
    }

    @Test
    fun `requestDelete_showsConfirmation_dismissHidesConfirmation`() = runTest {
        val repository = FakeBudgetRepository()
        val targetMonth = YearMonth(2026, 8)
        val setUseCase = SetMonthlyBudgetUseCase(repository)
        val getUseCase = GetMonthlyBudgetUseCase(repository)
        val deleteUseCase = DeleteMonthlyBudgetUseCase(repository)

        val viewModel = SetBudgetViewModel(
            setMonthlyBudgetUseCase = setUseCase,
            getMonthlyBudgetUseCase = getUseCase,
            deleteMonthlyBudgetUseCase = deleteUseCase,
            yearMonth = targetMonth,
            coroutineScope = this
        )

        assertFalse(viewModel.uiState.value.showDeleteConfirmation)

        viewModel.requestDelete()
        assertTrue(viewModel.uiState.value.showDeleteConfirmation)

        viewModel.dismissDelete()
        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
    }

    @Test
    fun `confirmDelete_invokesDeleteUseCaseAndEmitsDeleteSuccess`() = runTest {
        val repository = FakeBudgetRepository()
        val targetMonth = YearMonth(2026, 8)
        repository.setMonthlyBudget(MonthlyBudget(targetMonth, 200000L))

        val setUseCase = SetMonthlyBudgetUseCase(repository)
        val getUseCase = GetMonthlyBudgetUseCase(repository)
        val deleteUseCase = DeleteMonthlyBudgetUseCase(repository)

        val viewModel = SetBudgetViewModel(
            setMonthlyBudgetUseCase = setUseCase,
            getMonthlyBudgetUseCase = getUseCase,
            deleteMonthlyBudgetUseCase = deleteUseCase,
            yearMonth = targetMonth,
            coroutineScope = this
        )
        advanceUntilIdle()

        viewModel.requestDelete()
        viewModel.confirmDelete()

        val event = viewModel.events.first()
        assertEquals(SetBudgetEvent.DeleteSuccess, event)

        val deleted = repository.getMonthlyBudget(targetMonth).first()
        assertNull(deleted)
    }

    @Test
    fun `saveBudget with blank amount displays error message`() = runTest {
        val repository = FakeBudgetRepository()
        val setUseCase = SetMonthlyBudgetUseCase(repository)
        val getUseCase = GetMonthlyBudgetUseCase(repository)
        val deleteUseCase = DeleteMonthlyBudgetUseCase(repository)
        val targetMonth = YearMonth(2026, 8)

        val viewModel = SetBudgetViewModel(
            setMonthlyBudgetUseCase = setUseCase,
            getMonthlyBudgetUseCase = getUseCase,
            deleteMonthlyBudgetUseCase = deleteUseCase,
            yearMonth = targetMonth,
            coroutineScope = this
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
        val setUseCase = SetMonthlyBudgetUseCase(repository)
        val getUseCase = GetMonthlyBudgetUseCase(repository)
        val deleteUseCase = DeleteMonthlyBudgetUseCase(repository)
        val targetMonth = YearMonth(2026, 8)

        val viewModel = SetBudgetViewModel(
            setMonthlyBudgetUseCase = setUseCase,
            getMonthlyBudgetUseCase = getUseCase,
            deleteMonthlyBudgetUseCase = deleteUseCase,
            yearMonth = targetMonth,
            coroutineScope = this
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
        val setUseCase = SetMonthlyBudgetUseCase(repository)
        val getUseCase = GetMonthlyBudgetUseCase(repository)
        val deleteUseCase = DeleteMonthlyBudgetUseCase(repository)
        val targetMonth = YearMonth(2026, 8)

        val viewModel = SetBudgetViewModel(
            setMonthlyBudgetUseCase = setUseCase,
            getMonthlyBudgetUseCase = getUseCase,
            deleteMonthlyBudgetUseCase = deleteUseCase,
            yearMonth = targetMonth,
            coroutineScope = this
        )

        viewModel.saveBudget()
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.onAmountChanged("500")
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals("500", viewModel.uiState.value.amountText)
    }
}
