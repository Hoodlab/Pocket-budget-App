package org.hoods.pocket.presentation.budget.dashboard

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import org.hoods.pocket.domain.model.MonthlyBudget
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.usecase.FakeBudgetRepository
import org.hoods.pocket.domain.usecase.FakeTransactionRepository
import org.hoods.pocket.domain.usecase.GetMonthlyBudgetSummaryUseCase
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class BudgetDashboardUiTest {

    @Test
    fun emptyBudget_displaysEmptyStateAndSetActionButton() = runComposeUiTest {
        val budgetRepository = FakeBudgetRepository()
        val transactionRepository = FakeTransactionRepository()
        val targetMonth = YearMonth(2026, 8)
        val useCase = GetMonthlyBudgetSummaryUseCase(budgetRepository, transactionRepository)
        var setBudgetClicked = false

        setContent {
            val viewModel = BudgetDashboardViewModel(
                getMonthlyBudgetSummaryUseCase = useCase,
                initialMonth = targetMonth
            )
            BudgetDashboardScreen(
                viewModel = viewModel,
                onSetBudgetClick = { setBudgetClicked = true },
                onNavigateToTransactions = {}
            )
        }

        waitForIdle()

        onNodeWithText("No budget set for August 2026", substring = true).assertIsDisplayed()
        onNodeWithText("Set a monthly spending limit", substring = true).assertIsDisplayed()
        onNodeWithTag("set_monthly_budget_button").assertIsDisplayed()

        onNodeWithTag("set_monthly_budget_button").performClick()
        waitForIdle()

        assertTrue(setBudgetClicked)
    }

    @Test
    fun populatedBudget_displaysSpending_remaining_andIgnoresIncome() = runComposeUiTest {
        val budgetRepository = FakeBudgetRepository()
        val transactionRepository = FakeTransactionRepository()
        val targetMonth = YearMonth(2026, 8)

        // Set budget: $3,000.00
        budgetRepository.setMonthlyBudget(MonthlyBudget(targetMonth, 300000L))

        // Expense: $2,580.00
        transactionRepository.insertTransaction(
            Transaction(
                id = "tx-1",
                description = "Dining out",
                amount = 258000L,
                type = TransactionType.EXPENSE,
                date = Instant.parse("2026-08-15T12:00:00Z")
            )
        )

        // Income: $5,000.00 in same month (MUST NOT affect spending or remaining)
        transactionRepository.insertTransaction(
            Transaction(
                id = "tx-2",
                description = "Monthly Salary",
                amount = 500000L,
                type = TransactionType.INCOME,
                date = Instant.parse("2026-08-01T09:00:00Z")
            )
        )

        val useCase = GetMonthlyBudgetSummaryUseCase(budgetRepository, transactionRepository)

        setContent {
            val viewModel = BudgetDashboardViewModel(
                getMonthlyBudgetSummaryUseCase = useCase,
                initialMonth = targetMonth
            )
            BudgetDashboardScreen(
                viewModel = viewModel,
                onSetBudgetClick = {},
                onNavigateToTransactions = {}
            )
        }

        waitForIdle()

        // Total Budget Used section
        onNodeWithText("Total Budget Used").assertIsDisplayed()
        onNodeWithText("$2580.00", substring = true).assertIsDisplayed()
        onNodeWithText("/ $3000.00", substring = true).assertIsDisplayed()

        // Progress indicator
        onNodeWithTag("budget_progress_bar").assertIsDisplayed()
        onNodeWithText("86% Used").assertIsDisplayed()

        // Remaining amount: 3000.00 - 2580.00 = $420.00 left
        onNodeWithTag("remaining_amount_text").assertIsDisplayed()
        onNodeWithText("$420.00 left").assertIsDisplayed()

        // Health badge
        onNodeWithTag("budget_health_badge").assertIsDisplayed()
        onNodeWithText("Near Limit", substring = true).assertIsDisplayed()
    }

    @Test
    fun budgetDashboard_displaysTwentyPercentUsed_forTenThousandBudgetWithTwoThousandExpense() = runComposeUiTest {
        val budgetRepository = FakeBudgetRepository()
        val transactionRepository = FakeTransactionRepository()
        val targetMonth = YearMonth(2026, 9)

        // $100.00 budget (10000 cents)
        budgetRepository.setMonthlyBudget(MonthlyBudget(targetMonth, 10000L))

        // $20.00 expense (2000 cents)
        transactionRepository.insertTransaction(
            Transaction(
                id = "tx-1",
                description = "Lunch",
                amount = 2000L,
                type = TransactionType.EXPENSE,
                date = Instant.parse("2026-09-10T12:00:00Z")
            )
        )

        val useCase = GetMonthlyBudgetSummaryUseCase(budgetRepository, transactionRepository)

        setContent {
            val viewModel = BudgetDashboardViewModel(
                getMonthlyBudgetSummaryUseCase = useCase,
                initialMonth = targetMonth
            )
            BudgetDashboardScreen(
                viewModel = viewModel,
                onSetBudgetClick = {},
                onNavigateToTransactions = {}
            )
        }

        waitForIdle()

        onNodeWithText("20% Used").assertIsDisplayed()
        onNodeWithText("$20.00", substring = true).assertIsDisplayed()
        onNodeWithText("/ $100.00", substring = true).assertIsDisplayed()
        onNodeWithText("$80.00 left").assertIsDisplayed()
    }

    @Test
    fun monthBrowsing_navigatesMonthsAndShowsMonthSpecificBudget() = runComposeUiTest {
        val budgetRepository = FakeBudgetRepository()
        val transactionRepository = FakeTransactionRepository()
        val august = YearMonth(2026, 8)
        val september = YearMonth(2026, 9)

        budgetRepository.setMonthlyBudget(MonthlyBudget(august, 200000L))
        budgetRepository.setMonthlyBudget(MonthlyBudget(september, 400000L))

        val useCase = GetMonthlyBudgetSummaryUseCase(budgetRepository, transactionRepository)

        setContent {
            val viewModel = BudgetDashboardViewModel(
                getMonthlyBudgetSummaryUseCase = useCase,
                initialMonth = august
            )
            BudgetDashboardScreen(
                viewModel = viewModel,
                onSetBudgetClick = {},
                onNavigateToTransactions = {}
            )
        }

        waitForIdle()

        // August is shown
        onNodeWithText("August 2026").assertIsDisplayed()
        onNodeWithText("/ $2000.00", substring = true).assertIsDisplayed()

        // Next month -> September
        onNodeWithTag("next_month_button").performClick()
        waitForIdle()

        onNodeWithText("September 2026").assertIsDisplayed()
        onNodeWithText("/ $4000.00", substring = true).assertIsDisplayed()

        // Previous month -> August
        onNodeWithTag("previous_month_button").performClick()
        waitForIdle()

        onNodeWithText("August 2026").assertIsDisplayed()
        onNodeWithText("/ $2000.00", substring = true).assertIsDisplayed()
    }
}
