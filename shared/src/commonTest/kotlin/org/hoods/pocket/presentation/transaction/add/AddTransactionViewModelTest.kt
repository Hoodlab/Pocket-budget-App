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
    fun `saveTransaction persists valid transaction and emits SaveSuccess event`() = runTest {
        val repository = FakeTransactionRepository()
        val viewModel = AddTransactionViewModel(AddTransactionUseCase(repository), coroutineScope = this)

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
        assertFalse(state.isSaving)
        assertEquals("Disk full", state.errorMessage)
        assertEquals("Groceries", state.description)
        assertEquals("45.50", state.amountText)
        assertEquals(TransactionType.EXPENSE, state.selectedType)
    }
}
