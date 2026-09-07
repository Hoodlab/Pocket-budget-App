package org.hoods.pocket.presentation.transaction.edit

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.domain.usecase.DeleteTransactionUseCase
import org.hoods.pocket.domain.usecase.FakeTransactionRepository
import org.hoods.pocket.domain.usecase.GetTransactionByIdUseCase
import org.hoods.pocket.domain.usecase.UpdateTransactionUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class EditTransactionViewModelTest {

    private val repository = FakeTransactionRepository()
    private val getTransactionByIdUseCase = GetTransactionByIdUseCase(repository)
    private val updateTransactionUseCase = UpdateTransactionUseCase(repository)
    private val deleteTransactionUseCase = DeleteTransactionUseCase(repository)

    private fun TestScope.createViewModel() = EditTransactionViewModel(
        getTransactionByIdUseCase = getTransactionByIdUseCase,
        updateTransactionUseCase = updateTransactionUseCase,
        deleteTransactionUseCase = deleteTransactionUseCase,
        coroutineScope = this
    )

    @Test
    fun `loadTransaction populates UI state from repository`() = runTest {
        val tx = Transaction(
            id = "tx-123",
            description = "Dinner",
            amount = 3500L,
            type = TransactionType.EXPENSE,
            date = Instant.fromEpochMilliseconds(1717171717000L)
        )
        repository.insertTransaction(tx)

        val viewModel = createViewModel()
        viewModel.loadTransaction("tx-123")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Dinner", state.description)
        assertEquals("35.00", state.amountText)
        assertEquals(TransactionType.EXPENSE, state.selectedType)
        assertFalse(state.isLoading)
        assertFalse(state.isNotFound)
    }

    @Test
    fun `saveChanges updates existing transaction and emits SaveSuccess event`() = runTest {
        val original = Transaction(
            id = "tx-123",
            description = "Dinner",
            amount = 3500L,
            type = TransactionType.EXPENSE,
            date = Instant.fromEpochMilliseconds(1717171717000L)
        )
        repository.insertTransaction(original)

        val viewModel = createViewModel()
        viewModel.loadTransaction("tx-123")
        advanceUntilIdle()

        viewModel.onDescriptionChanged("Supermarket Dinner")
        viewModel.onAmountChanged("42.00")

        viewModel.saveChanges()
        advanceUntilIdle()

        val event = viewModel.events.first()
        assertEquals(EditTransactionEvent.SaveSuccess, event)
        assertNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSaving)

        val all = repository.getTransactions().first()
        assertEquals(1, all.size)
        assertEquals("Supermarket Dinner", all.first().description)
        assertEquals(4200L, all.first().amount)
        assertEquals("tx-123", all.first().id)
        assertEquals(original.date, all.first().date)
    }

    @Test
    fun `saveChanges prevents duplicate submission when already saving`() = runTest {
        val original = Transaction(
            id = "tx-123",
            description = "Dinner",
            amount = 3500L,
            type = TransactionType.EXPENSE,
            date = Instant.fromEpochMilliseconds(1717171717000L)
        )
        repository.insertTransaction(original)

        val viewModel = createViewModel()
        viewModel.loadTransaction("tx-123")
        advanceUntilIdle()

        viewModel.onDescriptionChanged("Supermarket Dinner")
        viewModel.onAmountChanged("42.00")

        viewModel.saveChanges()
        assertTrue(viewModel.uiState.value.isSaving)

        // Second call while isSaving is true is rejected
        viewModel.saveChanges()

        advanceUntilIdle()
        val all = repository.getTransactions().first()
        assertEquals(1, all.size)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `saveChanges failure preserves entered state, clears isSaving, and exposes error message`() = runTest {
        val original = Transaction(
            id = "tx-123",
            description = "Dinner",
            amount = 3500L,
            type = TransactionType.EXPENSE,
            date = Instant.fromEpochMilliseconds(1717171717000L)
        )
        repository.insertTransaction(original)

        val viewModel = createViewModel()
        viewModel.loadTransaction("tx-123")
        advanceUntilIdle()

        viewModel.onDescriptionChanged("Supermarket Dinner")
        viewModel.onAmountChanged("42.00")

        repository.shouldFail = true
        repository.failureMessage = "Write lock error"

        viewModel.saveChanges()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertEquals("Write lock error", state.errorMessage)
        assertEquals("Supermarket Dinner", state.description)
        assertEquals("42.00", state.amountText)
    }

    @Test
    fun `delete confirmation flow and confirming deletes transaction and emits DeleteSuccess event`() = runTest {
        val original = Transaction(
            id = "tx-123",
            description = "Dinner",
            amount = 3500L,
            type = TransactionType.EXPENSE,
            date = Instant.fromEpochMilliseconds(1717171717000L)
        )
        repository.insertTransaction(original)

        val viewModel = createViewModel()
        viewModel.loadTransaction("tx-123")
        advanceUntilIdle()

        viewModel.requestDelete()
        assertTrue(viewModel.uiState.value.showDeleteConfirmation)

        viewModel.dismissDelete()
        assertFalse(viewModel.uiState.value.showDeleteConfirmation)

        viewModel.confirmDelete()
        advanceUntilIdle()

        val event = viewModel.events.first()
        assertEquals(EditTransactionEvent.DeleteSuccess, event)
        assertNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isDeleting)

        val all = repository.getTransactions().first()
        assertTrue(all.isEmpty())
    }

    @Test
    fun `confirmDelete prevents duplicate deletion when already deleting`() = runTest {
        val original = Transaction(
            id = "tx-123",
            description = "Dinner",
            amount = 3500L,
            type = TransactionType.EXPENSE,
            date = Instant.fromEpochMilliseconds(1717171717000L)
        )
        repository.insertTransaction(original)

        val viewModel = createViewModel()
        viewModel.loadTransaction("tx-123")
        advanceUntilIdle()

        viewModel.confirmDelete()
        assertTrue(viewModel.uiState.value.isDeleting)

        // Second call while isDeleting is true is rejected
        viewModel.confirmDelete()

        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isDeleting)
    }

    @Test
    fun `confirmDelete failure clears isDeleting, preserves initialTransaction, and exposes error message`() = runTest {
        val original = Transaction(
            id = "tx-123",
            description = "Dinner",
            amount = 3500L,
            type = TransactionType.EXPENSE,
            date = Instant.fromEpochMilliseconds(1717171717000L)
        )
        repository.insertTransaction(original)

        val viewModel = createViewModel()
        viewModel.loadTransaction("tx-123")
        advanceUntilIdle()

        repository.shouldFail = true
        repository.failureMessage = "Delete constraint failed"

        viewModel.confirmDelete()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isDeleting)
        assertEquals("Delete constraint failed", state.errorMessage)
        assertNotNull(state.initialTransaction)
    }
}
