package org.hoods.pocket.presentation.transaction.add

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.domain.usecase.AddTransactionUseCase
import org.hoods.pocket.domain.usecase.FakeTransactionRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class AddTransactionViewModelTest {

    @Test
    fun `saveTransaction sets error when description is blank`() = runTest {
        val repository = FakeTransactionRepository()
        val viewModel = AddTransactionViewModel(AddTransactionUseCase(repository), coroutineScope = this)

        viewModel.onAmountChanged("45.50")
        viewModel.onDescriptionChanged("   ")

        viewModel.saveTransaction()
        advanceUntilIdle()

        assertEquals("Transaction description must not be blank", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `saveTransaction sets error when amount is invalid`() = runTest {
        val repository = FakeTransactionRepository()
        val viewModel = AddTransactionViewModel(AddTransactionUseCase(repository), coroutineScope = this)

        viewModel.onDescriptionChanged("Coffee")
        viewModel.onAmountChanged("0")

        viewModel.saveTransaction()
        advanceUntilIdle()

        assertEquals("Please enter a valid amount greater than zero", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `saveTransaction persists valid transaction with selectedDate and emits SaveSuccess event`() = runTest {
        val repository = FakeTransactionRepository()
        val customDate = Instant.parse("2026-08-15T10:00:00Z")
        val viewModel = AddTransactionViewModel(
            addTransactionUseCase = AddTransactionUseCase(repository),
            initialDate = customDate,
            coroutineScope = this
        )

        viewModel.onTypeSelected(TransactionType.EXPENSE)
        viewModel.onDescriptionChanged("Groceries")
        viewModel.onAmountChanged("45.50")

        viewModel.saveTransaction()
        advanceUntilIdle()

        val event = viewModel.events.first()
        assertEquals(AddTransactionEvent.SaveSuccess, event)
        assertNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSaving)

        val saved = repository.getTransactions().first()
        assertEquals(1, saved.size)
        assertEquals("Groceries", saved.first().description)
        assertEquals(4550L, saved.first().amount)
        assertEquals(TransactionType.EXPENSE, saved.first().type)
        assertEquals(customDate, saved.first().date)
    }

    @Test
    fun `date picker toggles and onDateSelected updates date and dateString`() = runTest {
        val repository = FakeTransactionRepository()
        val viewModel = AddTransactionViewModel(AddTransactionUseCase(repository), coroutineScope = this)

        assertFalse(viewModel.uiState.value.showDatePicker)

        viewModel.onOpenDatePicker()
        assertTrue(viewModel.uiState.value.showDatePicker)

        viewModel.onDismissDatePicker()
        assertFalse(viewModel.uiState.value.showDatePicker)

        val newDate = Instant.parse("2025-05-10T09:00:00Z")
        viewModel.onDateSelected(newDate)
        assertEquals(newDate, viewModel.uiState.value.selectedDate)
        assertEquals("May 10, 2025", viewModel.uiState.value.dateString)
        assertFalse(viewModel.uiState.value.showDatePicker)
    }

    @Test
    fun `saveTransaction prevents duplicate submission when already saving`() = runTest {
        val repository = FakeTransactionRepository()
        val viewModel = AddTransactionViewModel(AddTransactionUseCase(repository), coroutineScope = this)

        viewModel.onDescriptionChanged("Groceries")
        viewModel.onAmountChanged("45.50")

        viewModel.saveTransaction()
        assertTrue(viewModel.uiState.value.isSaving)

        // Second call while isSaving is true is rejected
        viewModel.saveTransaction()

        advanceUntilIdle()
        val saved = repository.getTransactions().first()
        assertEquals(1, saved.size)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `saveTransaction failure preserves entered state, clears isSaving, and exposes error message`() = runTest {
        val repository = FakeTransactionRepository()
        repository.shouldFail = true
        repository.failureMessage = "Disk full"
        val viewModel = AddTransactionViewModel(AddTransactionUseCase(repository), coroutineScope = this)

        viewModel.onTypeSelected(TransactionType.EXPENSE)
        viewModel.onDescriptionChanged("Groceries")
        viewModel.onAmountChanged("45.50")

        viewModel.saveTransaction()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Disk full", state.errorMessage)
        assertFalse(state.isSaving)
        assertEquals("Groceries", state.description)
        assertEquals("45.50", state.amountText)
        assertEquals(TransactionType.EXPENSE, state.selectedType)
    }
}
