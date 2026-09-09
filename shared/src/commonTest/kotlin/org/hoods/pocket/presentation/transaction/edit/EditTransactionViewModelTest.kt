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
    fun `loadTransaction populates UI state and date from repository`() = runTest {
        val txDate = Instant.parse("2024-05-31T16:08:37Z")
        val tx = Transaction(
            id = "tx-123",
            description = "Dinner",
            amount = 3500L,
            type = TransactionType.EXPENSE,
            date = txDate
        )
        repository.insertTransaction(tx)

        val viewModel = createViewModel()
        viewModel.loadTransaction("tx-123")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Dinner", state.description)
        assertEquals("35.00", state.amountText)
        assertEquals(TransactionType.EXPENSE, state.selectedType)
        assertEquals(txDate, state.selectedDate)
        assertEquals("May 31, 2024", state.dateString)
        assertFalse(state.isLoading)
        assertFalse(state.isNotFound)
    }

    @Test
    fun `saveChanges updates existing transaction date and emits SaveSuccess event`() = runTest {
        val originalDate = Instant.parse("2024-05-31T16:08:37Z")
        val original = Transaction(
            id = "tx-123",
            description = "Dinner",
            amount = 3500L,
            type = TransactionType.EXPENSE,
            date = originalDate
        )
        repository.insertTransaction(original)

        val viewModel = createViewModel()
        viewModel.loadTransaction("tx-123")
        advanceUntilIdle()

        val newDate = Instant.parse("2024-06-15T10:00:00Z")
        viewModel.onDescriptionChanged("Supermarket Dinner")
        viewModel.onAmountChanged("42.00")
        viewModel.onDateSelected(newDate)

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
        assertEquals(newDate, all.first().date)
    }

    @Test
    fun `date picker toggles and onDismissDatePicker closes dialog`() = runTest {
        val viewModel = createViewModel()
        assertFalse(viewModel.uiState.value.showDatePicker)

        viewModel.onOpenDatePicker()
        assertTrue(viewModel.uiState.value.showDatePicker)

        viewModel.onDismissDatePicker()
        assertFalse(viewModel.uiState.value.showDatePicker)
    }

    @Test
    fun `saveChanges prevents duplicate submission when already saving`() = runTest {
        val original = Transaction(
            id = "tx-123",
            description = "Dinner",
            amount = 3500L,
            type = TransactionType.EXPENSE,
            date = Instant.parse("2024-05-31T16:08:37Z")
        )
        repository.insertTransaction(original)

        val viewModel = createViewModel()
        viewModel.loadTransaction("tx-123")
        advanceUntilIdle()

        viewModel.saveChanges()
        assertTrue(viewModel.uiState.value.isSaving)

        viewModel.saveChanges() // Second call rejected
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `saveChanges failure preserves entered state, clears isSaving, and exposes error message`() = runTest {
        val original = Transaction(
            id = "tx-123",
            description = "Dinner",
            amount = 3500L,
            type = TransactionType.EXPENSE,
            date = Instant.parse("2024-05-31T16:08:37Z")
        )
        repository.insertTransaction(original)

        val viewModel = createViewModel()
        viewModel.loadTransaction("tx-123")
        advanceUntilIdle()

        repository.shouldFail = true
        repository.failureMessage = "Update error"

        viewModel.onDescriptionChanged("Altered Description")
        viewModel.onAmountChanged("99.99")

        viewModel.saveChanges()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Update error", state.errorMessage)
        assertFalse(state.isSaving)
        assertEquals("Altered Description", state.description)
        assertEquals("99.99", state.amountText)
    }

    @Test
    fun `delete confirmation workflow and duplicate deletion guard`() = runTest {
        val original = Transaction(
            id = "tx-123",
            description = "Dinner",
            amount = 3500L,
            type = TransactionType.EXPENSE,
            date = Instant.parse("2024-05-31T16:08:37Z")
        )
        repository.insertTransaction(original)

        val viewModel = createViewModel()
        viewModel.loadTransaction("tx-123")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
        viewModel.requestDelete()
        assertTrue(viewModel.uiState.value.showDeleteConfirmation)

        viewModel.dismissDelete()
        assertFalse(viewModel.uiState.value.showDeleteConfirmation)

        viewModel.confirmDelete()
        assertTrue(viewModel.uiState.value.isDeleting)

        advanceUntilIdle()
        val event = viewModel.events.first()
        assertEquals(EditTransactionEvent.DeleteSuccess, event)
        assertFalse(viewModel.uiState.value.isDeleting)
        assertTrue(repository.getTransactions().first().isEmpty())
    }

    @Test
    fun `confirmDelete failure clears isDeleting and exposes error message`() = runTest {
        val original = Transaction(
            id = "tx-123",
            description = "Dinner",
            amount = 3500L,
            type = TransactionType.EXPENSE,
            date = Instant.parse("2024-05-31T16:08:37Z")
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
        assertEquals("Delete constraint failed", state.errorMessage)
        assertFalse(state.isDeleting)
        assertNotNull(state.initialTransaction)
    }
}
