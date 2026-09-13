package org.hoods.pocket.presentation.budget.dashboard

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.domain.model.BudgetHealthStatus
import org.hoods.pocket.domain.model.MonthlyBudget
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.usecase.FakeBudgetRepository
import org.hoods.pocket.domain.usecase.FakeTransactionRepository
import org.hoods.pocket.domain.usecase.GetMonthlyBudgetSummaryUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class BudgetDashboardViewModelTest {

    @Test
    fun `uiState emits summary with budget and expense spending, ignoring income`() = runTest {
        val budgetRepository = FakeBudgetRepository()
        val transactionRepository = FakeTransactionRepository()
        val targetMonth = YearMonth(2026, 8)

        budgetRepository.setMonthlyBudget(MonthlyBudget(targetMonth, 300000L))
        transactionRepository.insertTransaction(
            Transaction(
                id = "1",
                description = "Dining",
                amount = 258000L,
                type = TransactionType.EXPENSE,
                date = Instant.parse("2026-08-15T12:00:00Z")
            )
        )
        // Income transaction in the same month must not affect spending
        transactionRepository.insertTransaction(
            Transaction(
                id = "2",
                description = "Salary",
                amount = 500000L,
                type = TransactionType.INCOME,
                date = Instant.parse("2026-08-01T09:00:00Z")
            )
        )

        val useCase = GetMonthlyBudgetSummaryUseCase(budgetRepository, transactionRepository)
        val viewModel = BudgetDashboardViewModel(
            getMonthlyBudgetSummaryUseCase = useCase,
            initialMonth = targetMonth,
            coroutineScope = backgroundScope
        )

        val state = viewModel.uiState.first { it.summary?.budget != null }
        assertTrue(state.hasBudget)
        assertEquals(targetMonth, state.selectedMonth)
        assertNotNull(state.summary)
        assertEquals(300000L, state.summary?.budget?.limitAmount)
        assertEquals(258000L, state.summary?.totalExpense)
        assertEquals(42000L, state.summary?.remainingAmount)
        assertEquals(BudgetHealthStatus.NEAR_LIMIT, state.summary?.status)
    }

    @Test
    fun `empty state when no budget exists for month`() = runTest {
        val budgetRepository = FakeBudgetRepository()
        val transactionRepository = FakeTransactionRepository()
        val targetMonth = YearMonth(2026, 8)

        val useCase = GetMonthlyBudgetSummaryUseCase(budgetRepository, transactionRepository)
        val viewModel = BudgetDashboardViewModel(
            getMonthlyBudgetSummaryUseCase = useCase,
            initialMonth = targetMonth,
            coroutineScope = backgroundScope
        )

        val state = viewModel.uiState.first { it.summary != null }
        assertFalse(state.hasBudget)
        assertNull(state.summary?.budget)
        assertEquals(0L, state.summary?.totalExpense)
        assertNull(state.summary?.remainingAmount)
    }

    @Test
    fun `navigation between months updates summary reactively`() = runTest {
        val budgetRepository = FakeBudgetRepository()
        val transactionRepository = FakeTransactionRepository()

        val august = YearMonth(2026, 8)
        val september = YearMonth(2026, 9)

        budgetRepository.setMonthlyBudget(MonthlyBudget(august, 200000L))
        budgetRepository.setMonthlyBudget(MonthlyBudget(september, 350000L))

        val useCase = GetMonthlyBudgetSummaryUseCase(budgetRepository, transactionRepository)
        val viewModel = BudgetDashboardViewModel(
            getMonthlyBudgetSummaryUseCase = useCase,
            initialMonth = august,
            coroutineScope = backgroundScope
        )

        val augState = viewModel.uiState.first { it.summary?.budget?.limitAmount == 200000L }
        assertEquals(august, augState.selectedMonth)
        assertEquals(200000L, augState.summary?.budget?.limitAmount)

        viewModel.onNextMonthClick()
        val sepState = viewModel.uiState.first { it.selectedMonth == september && it.summary?.budget?.limitAmount == 350000L }
        assertEquals(350000L, sepState.summary?.budget?.limitAmount)

        viewModel.onPreviousMonthClick()
        val backToAugState = viewModel.uiState.first { it.selectedMonth == august && it.summary?.budget?.limitAmount == 200000L }
        assertEquals(200000L, backToAugState.summary?.budget?.limitAmount)
    }

    @Test
    fun `onCurrentMonthClick resets to YearMonth now`() = runTest {
        val budgetRepository = FakeBudgetRepository()
        val transactionRepository = FakeTransactionRepository()
        val useCase = GetMonthlyBudgetSummaryUseCase(budgetRepository, transactionRepository)

        val pastMonth = YearMonth(2020, 1)
        val viewModel = BudgetDashboardViewModel(
            getMonthlyBudgetSummaryUseCase = useCase,
            initialMonth = pastMonth,
            coroutineScope = backgroundScope
        )

        assertFalse(viewModel.uiState.value.isCurrentMonth)

        viewModel.onCurrentMonthClick()
        val state = viewModel.uiState.first { it.isCurrentMonth }
        assertEquals(YearMonth.now(), state.selectedMonth)
        assertTrue(state.isCurrentMonth)
    }

    @Test
    fun `uiState emits categorySummaries when category budgets exist`() = runTest {
        val budgetRepository = FakeBudgetRepository()
        val categoryBudgetRepository = org.hoods.pocket.domain.usecase.FakeCategoryBudgetRepository()
        val transactionRepository = FakeTransactionRepository()
        val targetMonth = YearMonth(2026, 8)

        budgetRepository.setMonthlyBudget(MonthlyBudget(targetMonth, 300000L))
        categoryBudgetRepository.setCategoryBudget(
            org.hoods.pocket.domain.model.CategoryBudget(targetMonth, org.hoods.pocket.domain.model.TransactionCategory.GROCERIES, 60000L)
        )
        transactionRepository.insertTransaction(
            Transaction(
                id = "1",
                description = "Whole Foods",
                amount = 45000L,
                type = TransactionType.EXPENSE,
                date = Instant.parse("2026-08-15T12:00:00Z"),
                category = org.hoods.pocket.domain.model.TransactionCategory.GROCERIES
            )
        )

        val monthlyUseCase = GetMonthlyBudgetSummaryUseCase(budgetRepository, transactionRepository)
        val categoryUseCase = org.hoods.pocket.domain.usecase.GetCategoryBudgetSummariesUseCase(categoryBudgetRepository, transactionRepository)
        val viewModel = BudgetDashboardViewModel(
            getMonthlyBudgetSummaryUseCase = monthlyUseCase,
            getCategoryBudgetSummariesUseCase = categoryUseCase,
            initialMonth = targetMonth,
            coroutineScope = backgroundScope
        )

        val state = viewModel.uiState.first { it.categorySummaries.isNotEmpty() }
        assertTrue(state.hasCategoryBudgets)
        assertEquals(1, state.categorySummaries.size)
        val catSummary = state.categorySummaries.first()
        assertEquals(org.hoods.pocket.domain.model.TransactionCategory.GROCERIES, catSummary.category)
        assertEquals(60000L, catSummary.limitAmount)
        assertEquals(45000L, catSummary.spentAmount)
        assertEquals(15000L, catSummary.remainingAmount)
        assertEquals(0.75f, catSummary.progress)
        assertEquals(BudgetHealthStatus.ON_TRACK, catSummary.status)
    }

    @Test
    fun `adding expense updates categorySummaries reactively`() = runTest {
        val budgetRepository = FakeBudgetRepository()
        val categoryBudgetRepository = org.hoods.pocket.domain.usecase.FakeCategoryBudgetRepository()
        val transactionRepository = FakeTransactionRepository()
        val targetMonth = YearMonth(2026, 8)

        budgetRepository.setMonthlyBudget(MonthlyBudget(targetMonth, 200000L))
        categoryBudgetRepository.setCategoryBudget(
            org.hoods.pocket.domain.model.CategoryBudget(targetMonth, org.hoods.pocket.domain.model.TransactionCategory.DINING, 50000L)
        )

        val monthlyUseCase = GetMonthlyBudgetSummaryUseCase(budgetRepository, transactionRepository)
        val categoryUseCase = org.hoods.pocket.domain.usecase.GetCategoryBudgetSummariesUseCase(categoryBudgetRepository, transactionRepository)
        val viewModel = BudgetDashboardViewModel(
            getMonthlyBudgetSummaryUseCase = monthlyUseCase,
            getCategoryBudgetSummariesUseCase = categoryUseCase,
            initialMonth = targetMonth,
            coroutineScope = backgroundScope
        )

        val initialState = viewModel.uiState.first { it.categorySummaries.isNotEmpty() }
        assertEquals(0L, initialState.categorySummaries.first().spentAmount)
        assertEquals(BudgetHealthStatus.ON_TRACK, initialState.categorySummaries.first().status)

        // Insert expense reaching 80% (40000L) -> NEAR_LIMIT
        transactionRepository.insertTransaction(
            Transaction(
                id = "2",
                description = "Dinner",
                amount = 40000L,
                type = TransactionType.EXPENSE,
                date = Instant.parse("2026-08-10T19:00:00Z"),
                category = org.hoods.pocket.domain.model.TransactionCategory.DINING
            )
        )

        val updatedState = viewModel.uiState.first { it.categorySummaries.first().spentAmount == 40000L }
        assertEquals(40000L, updatedState.categorySummaries.first().spentAmount)
        assertEquals(10000L, updatedState.categorySummaries.first().remainingAmount)
        assertEquals(BudgetHealthStatus.NEAR_LIMIT, updatedState.categorySummaries.first().status)
    }
}
