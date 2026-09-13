package org.hoods.pocket.presentation.transaction.list

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.domain.model.CategoryBudget
import org.hoods.pocket.domain.model.MonthlyBudget
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.model.TransactionCategory
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.usecase.FakeBudgetRepository
import org.hoods.pocket.domain.usecase.FakeCategoryBudgetRepository
import org.hoods.pocket.domain.usecase.FakeTransactionRepository
import org.hoods.pocket.domain.usecase.GetCashFlowSummaryUseCase
import org.hoods.pocket.domain.usecase.GetCategoryBudgetSummariesUseCase
import org.hoods.pocket.domain.usecase.GetMonthlyBudgetSummaryUseCase
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

    @Test
    fun `uiState emits cashFlow, budgetSummary, and categorySummaries alongside transactions`() = runTest {
        val transactionRepository = FakeTransactionRepository()
        val budgetRepository = FakeBudgetRepository()
        val categoryBudgetRepository = FakeCategoryBudgetRepository()
        val targetMonth = YearMonth(2026, 8)

        // Income $3,000.00
        transactionRepository.insertTransaction(
            Transaction(
                id = "inc-1",
                description = "Consulting",
                amount = 300000L,
                type = TransactionType.INCOME,
                date = Instant.parse("2026-08-01T10:00:00Z")
            )
        )
        // Expense $500.00
        transactionRepository.insertTransaction(
            Transaction(
                id = "exp-1",
                description = "Groceries",
                amount = 50000L,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.GROCERIES,
                date = Instant.parse("2026-08-05T12:00:00Z")
            )
        )

        // Budget $2,000.00
        budgetRepository.setMonthlyBudget(MonthlyBudget(targetMonth, 200000L))
        // Category Budget Groceries $800.00
        categoryBudgetRepository.setCategoryBudget(
            CategoryBudget(targetMonth, TransactionCategory.GROCERIES, 80000L)
        )

        val txUseCase = GetTransactionsByMonthUseCase(transactionRepository)
        val cashFlowUseCase = GetCashFlowSummaryUseCase(transactionRepository)
        val budgetUseCase = GetMonthlyBudgetSummaryUseCase(budgetRepository, transactionRepository)
        val categoryUseCase = GetCategoryBudgetSummariesUseCase(categoryBudgetRepository, transactionRepository)

        val viewModel = TransactionListViewModel(
            getTransactionsByMonthUseCase = txUseCase,
            getCashFlowSummaryUseCase = cashFlowUseCase,
            getMonthlyBudgetSummaryUseCase = budgetUseCase,
            getCategoryBudgetSummariesUseCase = categoryUseCase,
            initialMonth = targetMonth,
            coroutineScope = backgroundScope
        )

        val state = viewModel.uiState.first { it.cashFlow != null && it.cashFlow.income > 0 }
        assertEquals(2, state.transactions.size)
        // Cash flow: income 300000, expense 50000, netBalance 250000
        assertEquals(300000L, state.cashFlow?.income)
        assertEquals(50000L, state.cashFlow?.expense)
        assertEquals(250000L, state.cashFlow?.netBalance)

        // Budget summary: limit 200000, totalExpense 50000, remaining 150000
        assertEquals(200000L, state.budgetSummary?.budget?.limitAmount)
        assertEquals(50000L, state.budgetSummary?.totalExpense)
        assertEquals(150000L, state.budgetSummary?.remainingAmount)

        // Category summaries: Groceries limit 80000, spent 50000, remaining 30000
        assertEquals(1, state.categorySummaries.size)
        val groceries = state.categorySummaries.first()
        assertEquals(TransactionCategory.GROCERIES, groceries.category)
        assertEquals(80000L, groceries.limitAmount)
        assertEquals(50000L, groceries.spentAmount)
        assertEquals(30000L, groceries.remainingAmount)
    }

    @Test
    fun `adding income updates cashFlow reactively`() = runTest {
        val transactionRepository = FakeTransactionRepository()
        val targetMonth = YearMonth(2026, 8)

        val txUseCase = GetTransactionsByMonthUseCase(transactionRepository)
        val cashFlowUseCase = GetCashFlowSummaryUseCase(transactionRepository)

        val viewModel = TransactionListViewModel(
            getTransactionsByMonthUseCase = txUseCase,
            getCashFlowSummaryUseCase = cashFlowUseCase,
            initialMonth = targetMonth,
            coroutineScope = backgroundScope
        )

        val initialState = viewModel.uiState.first { it.cashFlow != null }
        assertEquals(0L, initialState.cashFlow?.income)
        assertEquals(0L, initialState.cashFlow?.expense)
        assertEquals(0L, initialState.cashFlow?.netBalance)

        transactionRepository.insertTransaction(
            Transaction(
                id = "bonus",
                description = "Freelance bonus",
                amount = 65000L,
                type = TransactionType.INCOME,
                category = TransactionCategory.FREELANCE,
                date = Instant.parse("2026-08-20T10:00:00Z")
            )
        )

        val updatedState = viewModel.uiState.first { it.cashFlow != null && it.cashFlow.income == 65000L }
        assertEquals(65000L, updatedState.cashFlow?.income)
        assertEquals(0L, updatedState.cashFlow?.expense)
        assertEquals(65000L, updatedState.cashFlow?.netBalance)
    }

    @Test
    fun `no-budget month still emits valid cashFlow`() = runTest {
        val transactionRepository = FakeTransactionRepository()
        val targetMonth = YearMonth(2026, 8)

        transactionRepository.insertTransaction(
            Transaction(
                id = "exp",
                description = "Miscellaneous",
                amount = 15000L,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.UNCATEGORIZED,
                date = Instant.parse("2026-08-10T12:00:00Z")
            )
        )

        val txUseCase = GetTransactionsByMonthUseCase(transactionRepository)
        val cashFlowUseCase = GetCashFlowSummaryUseCase(transactionRepository)

        val viewModel = TransactionListViewModel(
            getTransactionsByMonthUseCase = txUseCase,
            getCashFlowSummaryUseCase = cashFlowUseCase,
            initialMonth = targetMonth,
            coroutineScope = backgroundScope
        )

        val state = viewModel.uiState.first { it.cashFlow != null && it.cashFlow.expense == 15000L }
        assertEquals(null, state.budgetSummary)
        assertEquals(0L, state.cashFlow?.income)
        assertEquals(15000L, state.cashFlow?.expense)
        assertEquals(-15000L, state.cashFlow?.netBalance)
    }
}
