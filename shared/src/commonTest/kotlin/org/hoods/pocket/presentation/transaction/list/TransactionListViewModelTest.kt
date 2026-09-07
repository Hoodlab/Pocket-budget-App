package org.hoods.pocket.presentation.transaction.list

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.domain.usecase.FakeTransactionRepository
import org.hoods.pocket.domain.usecase.GetTransactionsUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Instant

class TransactionListViewModelTest {

    @Test
    fun `uiState emits transactions from GetTransactionsUseCase`() = runTest {
        val repository = FakeTransactionRepository()
        val tx = Transaction(
            id = "1",
            description = "Groceries",
            amount = 4550L,
            type = TransactionType.EXPENSE,
            date = Instant.fromEpochMilliseconds(1717171717000L)
        )
        repository.insertTransaction(tx)

        val useCase = GetTransactionsUseCase(repository)
        val viewModel = TransactionListViewModel(useCase, coroutineScope = backgroundScope)

        val state = viewModel.uiState.first { it.transactions.isNotEmpty() }
        assertEquals(1, state.transactions.size)
        assertEquals("Groceries", state.transactions.first().description)
        assertFalse(state.isLoading)
    }
}
