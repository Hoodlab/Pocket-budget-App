package org.hoods.pocket.presentation.transaction.list

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.usecase.FakeTransactionRepository
import org.hoods.pocket.domain.usecase.GetTransactionsByMonthUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class TransactionListViewModelTest {

    @Test
    fun `uiState emits transactions filtered to selectedMonth`() = runTest {
        val repository = FakeTransactionRepository()
        val mayTx = Transaction(
            id = "1",
            description = "Groceries",
            amount = 4550L,
            type = TransactionType.EXPENSE,
            date = Instant.parse("2024-05-15T12:00:00Z")
        )
        val juneTx = Transaction(
            id = "2",
            description = "Salary",
            amount = 300000L,
            type = TransactionType.INCOME,
            date = Instant.parse("2024-06-01T12:00:00Z")
        )
        repository.insertTransaction(mayTx)
        repository.insertTransaction(juneTx)

        val useCase = GetTransactionsByMonthUseCase(repository)
        val viewModel = TransactionListViewModel(
            getTransactionsByMonthUseCase = useCase,
            initialMonth = YearMonth(2024, 5),
            coroutineScope = backgroundScope
        )

        val state = viewModel.uiState.first { it.transactions.isNotEmpty() }
        assertEquals(1, state.transactions.size)
        assertEquals("Groceries", state.transactions.first().description)
        assertEquals(YearMonth(2024, 5), state.selectedMonth)
        assertFalse(state.isCurrentMonth)
        assertFalse(state.isLoading)
    }

    @Test
    fun `onNextMonth and onPreviousMonth navigate months and filter transactions`() = runTest {
        val repository = FakeTransactionRepository()
        val mayTx = Transaction(
            id = "1",
            description = "May Expense",
            amount = 1000L,
            type = TransactionType.EXPENSE,
            date = Instant.parse("2024-05-15T12:00:00Z")
        )
        val juneTx = Transaction(
            id = "2",
            description = "June Income",
            amount = 2000L,
            type = TransactionType.INCOME,
            date = Instant.parse("2024-06-15T12:00:00Z")
        )
        repository.insertTransaction(mayTx)
        repository.insertTransaction(juneTx)

        val useCase = GetTransactionsByMonthUseCase(repository)
        val viewModel = TransactionListViewModel(
            getTransactionsByMonthUseCase = useCase,
            initialMonth = YearMonth(2024, 5),
            coroutineScope = backgroundScope
        )

        val mayState = viewModel.uiState.first { it.transactions.isNotEmpty() }
        assertEquals("May Expense", mayState.transactions.first().description)

        viewModel.onNextMonth()
        val juneState = viewModel.uiState.first { it.selectedMonth == YearMonth(2024, 6) }
        assertEquals(1, juneState.transactions.size)
        assertEquals("June Income", juneState.transactions.first().description)

        viewModel.onPreviousMonth()
        val backToMayState = viewModel.uiState.first { it.selectedMonth == YearMonth(2024, 5) }
        assertEquals(1, backToMayState.transactions.size)
        assertEquals("May Expense", backToMayState.transactions.first().description)
    }

    @Test
    fun `onCurrentMonth resets selectedMonth to YearMonth now`() = runTest {
        val repository = FakeTransactionRepository()
        val useCase = GetTransactionsByMonthUseCase(repository)
        val viewModel = TransactionListViewModel(
            getTransactionsByMonthUseCase = useCase,
            initialMonth = YearMonth(2020, 1),
            coroutineScope = backgroundScope
        )

        assertFalse(viewModel.uiState.value.isCurrentMonth)

        viewModel.onCurrentMonth()
        val state = viewModel.uiState.first { it.isCurrentMonth }
        assertEquals(YearMonth.now(), state.selectedMonth)
        assertTrue(state.isCurrentMonth)
    }
}
