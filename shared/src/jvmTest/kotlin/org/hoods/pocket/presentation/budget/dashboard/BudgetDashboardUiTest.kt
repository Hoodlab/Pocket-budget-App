package org.hoods.pocket.presentation.budget.dashboard

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
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

    @Test
    fun populatedBudget_editButtonTriggersOnSetBudgetClick() = runComposeUiTest {
        val budgetRepository = FakeBudgetRepository()
        val transactionRepository = FakeTransactionRepository()
        val targetMonth = YearMonth(2026, 9)
        var editClicked = false

        budgetRepository.setMonthlyBudget(MonthlyBudget(targetMonth, 250000L))

        val useCase = GetMonthlyBudgetSummaryUseCase(budgetRepository, transactionRepository)

        setContent {
            val viewModel = BudgetDashboardViewModel(
                getMonthlyBudgetSummaryUseCase = useCase,
                initialMonth = targetMonth
            )
            BudgetDashboardScreen(
                viewModel = viewModel,
                onSetBudgetClick = { editClicked = true },
                onNavigateToTransactions = {}
            )
        }

        waitForIdle()

        onNodeWithTag("edit_budget_button").assertIsDisplayed()
        onNodeWithTag("edit_budget_button").performClick()
        waitForIdle()

        assertTrue(editClicked)
    }

    @Test
    fun populatedCategoryBudgets_displaysCategoryCardsWithSpendingRemainingAndHealthBadge() = runComposeUiTest {
        val budgetRepository = FakeBudgetRepository()
        val categoryBudgetRepository = org.hoods.pocket.domain.usecase.FakeCategoryBudgetRepository()
        val transactionRepository = FakeTransactionRepository()
        val targetMonth = YearMonth(2026, 8)

        // Set monthly budget: $3,000.00
        budgetRepository.setMonthlyBudget(MonthlyBudget(targetMonth, 300000L))

        // Set category budget: Groceries $600.00
        categoryBudgetRepository.setCategoryBudget(
            org.hoods.pocket.domain.model.CategoryBudget(targetMonth, org.hoods.pocket.domain.model.TransactionCategory.GROCERIES, 60000L)
        )

        // Expense: $450.00
        transactionRepository.insertTransaction(
            Transaction(
                id = "tx-groc",
                description = "Supermarket",
                amount = 45000L,
                type = TransactionType.EXPENSE,
                date = Instant.parse("2026-08-15T12:00:00Z"),
                category = org.hoods.pocket.domain.model.TransactionCategory.GROCERIES
            )
        )

        val monthlyUseCase = GetMonthlyBudgetSummaryUseCase(budgetRepository, transactionRepository)
        val categoryUseCase = org.hoods.pocket.domain.usecase.GetCategoryBudgetSummariesUseCase(categoryBudgetRepository, transactionRepository)

        setContent {
            val viewModel = BudgetDashboardViewModel(
                getMonthlyBudgetSummaryUseCase = monthlyUseCase,
                getCategoryBudgetSummariesUseCase = categoryUseCase,
                initialMonth = targetMonth
            )
            BudgetDashboardScreen(
                viewModel = viewModel,
                onSetBudgetClick = {},
                onNavigateToTransactions = {}
            )
        }

        waitForIdle()

        onNodeWithTag("category_budgets_title").assertIsDisplayed()
        onNodeWithTag("category_budget_card_GROCERIES").assertIsDisplayed()
        onNodeWithTag("category_name_GROCERIES").assertIsDisplayed()
        onNodeWithText("Groceries", substring = true).assertIsDisplayed()
        onNodeWithTag("category_amounts_GROCERIES").assertIsDisplayed()
        onNodeWithTag("category_remaining_GROCERIES").assertIsDisplayed()
        onNodeWithTag("category_remaining_GROCERIES").assertTextContains("150.00", substring = true)
    }

    @Test
    fun nearLimitAndOverBudgetCategoryCards_displayCorrectHealthBadgesAndRemainingOrOverAmounts() = runComposeUiTest {
        val budgetRepository = FakeBudgetRepository()
        val categoryBudgetRepository = org.hoods.pocket.domain.usecase.FakeCategoryBudgetRepository()
        val transactionRepository = FakeTransactionRepository()
        val targetMonth = YearMonth(2026, 8)

        budgetRepository.setMonthlyBudget(MonthlyBudget(targetMonth, 300000L))

        // Transport: $200.00 limit, $160.00 spent (80% -> Near Limit)
        categoryBudgetRepository.setCategoryBudget(
            org.hoods.pocket.domain.model.CategoryBudget(targetMonth, org.hoods.pocket.domain.model.TransactionCategory.TRANSPORT, 20000L)
        )
        transactionRepository.insertTransaction(
            Transaction(
                id = "tx-trans",
                description = "Gas & Train",
                amount = 16000L,
                type = TransactionType.EXPENSE,
                date = Instant.parse("2026-08-10T12:00:00Z"),
                category = org.hoods.pocket.domain.model.TransactionCategory.TRANSPORT
            )
        )

        // Entertainment: $300.00 limit, $350.00 spent (116% -> Over Budget)
        categoryBudgetRepository.setCategoryBudget(
            org.hoods.pocket.domain.model.CategoryBudget(targetMonth, org.hoods.pocket.domain.model.TransactionCategory.ENTERTAINMENT, 30000L)
        )
        transactionRepository.insertTransaction(
            Transaction(
                id = "tx-ent",
                description = "Concert",
                amount = 35000L,
                type = TransactionType.EXPENSE,
                date = Instant.parse("2026-08-12T19:00:00Z"),
                category = org.hoods.pocket.domain.model.TransactionCategory.ENTERTAINMENT
            )
        )

        val monthlyUseCase = GetMonthlyBudgetSummaryUseCase(budgetRepository, transactionRepository)
        val categoryUseCase = org.hoods.pocket.domain.usecase.GetCategoryBudgetSummariesUseCase(categoryBudgetRepository, transactionRepository)

        setContent {
            val viewModel = BudgetDashboardViewModel(
                getMonthlyBudgetSummaryUseCase = monthlyUseCase,
                getCategoryBudgetSummariesUseCase = categoryUseCase,
                initialMonth = targetMonth
            )
            BudgetDashboardScreen(
                viewModel = viewModel,
                onSetBudgetClick = {},
                onNavigateToTransactions = {}
            )
        }

        waitForIdle()

        // Transport assertions (Near Limit)
        onNodeWithTag("category_budget_card_TRANSPORT").assertIsDisplayed()
        onNodeWithTag("category_remaining_TRANSPORT").assertIsDisplayed()
        onNodeWithTag("category_remaining_TRANSPORT").assertTextContains("40.00", substring = true)
        onNodeWithTag("category_health_badge_TRANSPORT").assertIsDisplayed()
        onNodeWithText("Near Limit", substring = true).assertIsDisplayed()

        // Entertainment assertions (Over Budget)
        onNodeWithTag("category_budget_card_ENTERTAINMENT").assertIsDisplayed()
        onNodeWithTag("category_remaining_ENTERTAINMENT").assertIsDisplayed()
        onNodeWithTag("category_remaining_ENTERTAINMENT").assertTextContains("50.00", substring = true)
        onNodeWithTag("category_health_badge_ENTERTAINMENT").assertIsDisplayed()
        onNodeWithText("Over Budget", substring = true).assertIsDisplayed()
    }
}
