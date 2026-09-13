package org.hoods.pocket.presentation.budget.set

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.domain.model.CategoryBudget
import org.hoods.pocket.domain.model.MonthlyBudget
import org.hoods.pocket.domain.model.TransactionCategory
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.usecase.DeleteMonthlyBudgetUseCase
import org.hoods.pocket.domain.usecase.FakeBudgetRepository
import org.hoods.pocket.domain.usecase.FakeCategoryBudgetRepository
import org.hoods.pocket.domain.usecase.GetCategoryBudgetsUseCase
import org.hoods.pocket.domain.usecase.GetMonthlyBudgetUseCase
import org.hoods.pocket.domain.usecase.RemoveCategoryBudgetUseCase
import org.hoods.pocket.domain.usecase.SaveCategoryAllocationsUseCase
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

    @Test
    fun `categoryAllocations_calculatesRemainingAmountInCents`() = runTest {
        val targetMonth = YearMonth(2026, 8)
        val viewModel = SetBudgetViewModel(
            setMonthlyBudgetUseCase = SetMonthlyBudgetUseCase(FakeBudgetRepository()),
            getMonthlyBudgetUseCase = GetMonthlyBudgetUseCase(FakeBudgetRepository()),
            deleteMonthlyBudgetUseCase = DeleteMonthlyBudgetUseCase(FakeBudgetRepository()),
            yearMonth = targetMonth,
            coroutineScope = this
        )

        viewModel.onAmountChanged("3000.00") // $3,000.00 = 300,000 cents
        assertEquals(300000L, viewModel.uiState.value.totalBudgetCents)
        assertEquals(300000L, viewModel.uiState.value.remainingCents)

        // Allocate $600 to Groceries
        viewModel.onCategoryAllocationChanged(org.hoods.pocket.domain.model.TransactionCategory.GROCERIES, "600.00")
        assertEquals(60000L, viewModel.uiState.value.totalAllocatedCents)
        assertEquals(240000L, viewModel.uiState.value.remainingCents) // $2,400.00 left

        // Allocate $400 to Dining
        viewModel.onCategoryAllocationChanged(org.hoods.pocket.domain.model.TransactionCategory.DINING, "400.00")
        assertEquals(100000L, viewModel.uiState.value.totalAllocatedCents)
        assertEquals(200000L, viewModel.uiState.value.remainingCents) // $2,000.00 left
    }

    @Test
    fun `overAllocation_flagsIsOverAllocatedAndPreventsSave`() = runTest {
        val budgetRepo = FakeBudgetRepository()
        val categoryRepo = FakeCategoryBudgetRepository()
        val targetMonth = YearMonth(2026, 8)

        val viewModel = SetBudgetViewModel(
            setMonthlyBudgetUseCase = SetMonthlyBudgetUseCase(budgetRepo),
            getMonthlyBudgetUseCase = GetMonthlyBudgetUseCase(budgetRepo),
            deleteMonthlyBudgetUseCase = DeleteMonthlyBudgetUseCase(budgetRepo),
            getCategoryBudgetsUseCase = GetCategoryBudgetsUseCase(categoryRepo),
            saveCategoryAllocationsUseCase = SaveCategoryAllocationsUseCase(budgetRepo, categoryRepo),
            removeCategoryBudgetUseCase = RemoveCategoryBudgetUseCase(categoryRepo),
            yearMonth = targetMonth,
            coroutineScope = this
        )

        viewModel.onAmountChanged("1000.00") // $1,000.00
        viewModel.onCategoryAllocationChanged(org.hoods.pocket.domain.model.TransactionCategory.GROCERIES, "700.00")
        viewModel.onCategoryAllocationChanged(org.hoods.pocket.domain.model.TransactionCategory.DINING, "500.00") // Total $1,200.00 > $1,000.00

        assertTrue(viewModel.uiState.value.isOverAllocated)
        assertEquals(20000L, viewModel.uiState.value.overAllocatedCents) // $200.00 over

        viewModel.saveBudget()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("exceed", ignoreCase = true))
        assertNull(budgetRepo.getMonthlyBudget(targetMonth).first())
    }

    @Test
    fun `saveBudget_withCategoryAllocations_savesBothAndEmitsSaveSuccess`() = runTest {
        val budgetRepo = FakeBudgetRepository()
        val categoryRepo = FakeCategoryBudgetRepository()
        val targetMonth = YearMonth(2026, 8)

        val viewModel = SetBudgetViewModel(
            setMonthlyBudgetUseCase = SetMonthlyBudgetUseCase(budgetRepo),
            getMonthlyBudgetUseCase = GetMonthlyBudgetUseCase(budgetRepo),
            deleteMonthlyBudgetUseCase = DeleteMonthlyBudgetUseCase(budgetRepo),
            getCategoryBudgetsUseCase = GetCategoryBudgetsUseCase(categoryRepo),
            saveCategoryAllocationsUseCase = SaveCategoryAllocationsUseCase(budgetRepo, categoryRepo),
            removeCategoryBudgetUseCase = RemoveCategoryBudgetUseCase(categoryRepo),
            yearMonth = targetMonth,
            coroutineScope = this
        )

        viewModel.onAmountChanged("2000.00")
        viewModel.onCategoryAllocationChanged(org.hoods.pocket.domain.model.TransactionCategory.GROCERIES, "600.00")
        viewModel.onCategoryAllocationChanged(org.hoods.pocket.domain.model.TransactionCategory.DINING, "400.00")

        viewModel.saveBudget()

        val event = viewModel.events.first()
        assertEquals(SetBudgetEvent.SaveSuccess, event)

        val savedMonthly = budgetRepo.getMonthlyBudget(targetMonth).first()
        assertNotNull(savedMonthly)
        assertEquals(200000L, savedMonthly.limitAmount)

        val savedCategories = categoryRepo.getCategoryBudgets(targetMonth).first()
        assertEquals(2, savedCategories.size)
        assertEquals(60000L, savedCategories.find { it.category == org.hoods.pocket.domain.model.TransactionCategory.GROCERIES }?.limitAmount)
        assertEquals(40000L, savedCategories.find { it.category == org.hoods.pocket.domain.model.TransactionCategory.DINING }?.limitAmount)
    }

    @Test
    fun `onResetAllocations_clearsAllAllocations`() = runTest {
        val targetMonth = YearMonth(2026, 8)
        val viewModel = SetBudgetViewModel(
            setMonthlyBudgetUseCase = SetMonthlyBudgetUseCase(FakeBudgetRepository()),
            getMonthlyBudgetUseCase = GetMonthlyBudgetUseCase(FakeBudgetRepository()),
            deleteMonthlyBudgetUseCase = DeleteMonthlyBudgetUseCase(FakeBudgetRepository()),
            yearMonth = targetMonth,
            coroutineScope = this
        )

        viewModel.onCategoryAllocationChanged(org.hoods.pocket.domain.model.TransactionCategory.GROCERIES, "500.00")
        viewModel.onCategoryAllocationChanged(org.hoods.pocket.domain.model.TransactionCategory.DINING, "300.00")
        assertEquals(2, viewModel.uiState.value.categoryAllocations.size)

        viewModel.onResetAllocations()
        assertTrue(viewModel.uiState.value.categoryAllocations.isEmpty())
        assertEquals(0L, viewModel.uiState.value.totalAllocatedCents)
    }

    @Test
    fun `existingCategoryBudgets_areLoadedIntoUiState`() = runTest {
        val budgetRepo = FakeBudgetRepository()
        val categoryRepo = FakeCategoryBudgetRepository()
        val targetMonth = YearMonth(2026, 8)

        budgetRepo.setMonthlyBudget(org.hoods.pocket.domain.model.MonthlyBudget(targetMonth, 250000L))
        categoryRepo.setCategoryBudget(
            org.hoods.pocket.domain.model.CategoryBudget(targetMonth, org.hoods.pocket.domain.model.TransactionCategory.GROCERIES, 60000L)
        )
        categoryRepo.setCategoryBudget(
            org.hoods.pocket.domain.model.CategoryBudget(targetMonth, org.hoods.pocket.domain.model.TransactionCategory.TRANSPORT, 20000L)
        )

        val viewModel = SetBudgetViewModel(
            setMonthlyBudgetUseCase = SetMonthlyBudgetUseCase(budgetRepo),
            getMonthlyBudgetUseCase = GetMonthlyBudgetUseCase(budgetRepo),
            deleteMonthlyBudgetUseCase = DeleteMonthlyBudgetUseCase(budgetRepo),
            getCategoryBudgetsUseCase = GetCategoryBudgetsUseCase(categoryRepo),
            saveCategoryAllocationsUseCase = SaveCategoryAllocationsUseCase(budgetRepo, categoryRepo),
            removeCategoryBudgetUseCase = RemoveCategoryBudgetUseCase(categoryRepo),
            yearMonth = targetMonth,
            coroutineScope = this
        )
        advanceUntilIdle()

        assertEquals("2500.00", viewModel.uiState.value.amountText)
        assertEquals("600.00", viewModel.uiState.value.categoryAllocations[org.hoods.pocket.domain.model.TransactionCategory.GROCERIES])
        assertEquals("200.00", viewModel.uiState.value.categoryAllocations[org.hoods.pocket.domain.model.TransactionCategory.TRANSPORT])
        assertEquals(80000L, viewModel.uiState.value.totalAllocatedCents)
        assertEquals(170000L, viewModel.uiState.value.remainingCents)
    }
}
