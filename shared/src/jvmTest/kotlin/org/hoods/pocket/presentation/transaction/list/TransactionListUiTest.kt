package org.hoods.pocket.presentation.transaction.list

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
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
import org.hoods.pocket.domain.usecase.GetCategoryBudgetSummariesUseCase
import org.hoods.pocket.domain.usecase.GetCashFlowSummaryUseCase
import org.hoods.pocket.domain.usecase.GetMonthlyBudgetSummaryUseCase
import org.hoods.pocket.domain.usecase.GetTransactionsByMonthUseCase
import kotlin.test.Test
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class TransactionListUiTest {

    @Test
    fun monthBrowsing_navigatesMonthsAndFiltersTransactionsCorrectly() = runComposeUiTest {
        val repository = FakeTransactionRepository()
        val currentMonth = YearMonth.now()
        val prevMonth = currentMonth.previous()

        // Transaction in current month
        val currentMonthTx = Transaction(
            id = "tx-curr",
            description = "Current Month Groceries",
            amount = 5000L,
            type = TransactionType.EXPENSE,
            date = Instant.parse(String.format("%04d-%02d-15T10:00:00Z", currentMonth.year, currentMonth.month))
        )
        // Transaction in previous month
        val prevMonthTx = Transaction(
            id = "tx-prev",
            description = "Previous Month Rent",
            amount = 120000L,
            type = TransactionType.EXPENSE,
            date = Instant.parse(String.format("%04d-%02d-10T10:00:00Z", prevMonth.year, prevMonth.month))
        )
        runTest {
            repository.insertTransaction(currentMonthTx)
            repository.insertTransaction(prevMonthTx)
        }

        val viewModel = TransactionListViewModel(
            getTransactionsByMonthUseCase = GetTransactionsByMonthUseCase(repository),
            initialMonth = currentMonth
        )

        setContent {
            TransactionListScreen(
                viewModel = viewModel,
                onAddTransactionClick = {},
                onTransactionClick = {}
            )
        }

        waitForIdle()

        // 1. Initial State: Current month is displayed, showing only currentMonthTx
        onNodeWithTag("selected_month_text").assertIsDisplayed()
        onNodeWithText(currentMonth.format()).assertIsDisplayed()
        onNodeWithText("Current Month Groceries").assertIsDisplayed()
        onNodeWithText("Previous Month Rent").assertDoesNotExist()
        // When in current month, "Today" chip is not visible
        onNodeWithTag("current_month_button").assertDoesNotExist()

        // 2. Navigate to Previous Month
        onNodeWithTag("previous_month_button").performClick()
        waitForIdle()

        onNodeWithText(prevMonth.format()).assertIsDisplayed()
        onNodeWithText("Previous Month Rent").assertIsDisplayed()
        onNodeWithText("Current Month Groceries").assertDoesNotExist()
        // Now "Today" button is visible
        onNodeWithTag("current_month_button").assertIsDisplayed()

        // 3. Return to Current Month via "Today" button
        onNodeWithTag("current_month_button").performClick()
        waitForIdle()

        onNodeWithText(currentMonth.format()).assertIsDisplayed()
        onNodeWithText("Current Month Groceries").assertIsDisplayed()
        onNodeWithText("Previous Month Rent").assertDoesNotExist()
        onNodeWithTag("current_month_button").assertDoesNotExist()

        // 4. Navigate to Next Month
        val nextMonth = currentMonth.next()
        onNodeWithTag("next_month_button").performClick()
        waitForIdle()

        onNodeWithText(nextMonth.format()).assertIsDisplayed()
        onNodeWithText("No transactions in ${nextMonth.format()}\nTap + to add one").assertIsDisplayed()
        onNodeWithTag("current_month_button").assertIsDisplayed()
    }

    @Test
    fun cashFlowAndBudgetHealth_displayedWhenDataPresent() = runComposeUiTest {
        val currentMonth = YearMonth.now()
        val transactionRepository = FakeTransactionRepository()
        val budgetRepository = FakeBudgetRepository()
        val categoryBudgetRepository = FakeCategoryBudgetRepository()

        val incomeTx = Transaction(
            id = "tx-income",
            description = "Salary",
            amount = 500000L,
            type = TransactionType.INCOME,
            category = TransactionCategory.SALARY,
            date = Instant.parse(String.format("%04d-%02d-01T10:00:00Z", currentMonth.year, currentMonth.month))
        )
        val expenseTx = Transaction(
            id = "tx-expense",
            description = "Groceries",
            amount = 150000L,
            type = TransactionType.EXPENSE,
            category = TransactionCategory.GROCERIES,
            date = Instant.parse(String.format("%04d-%02d-05T10:00:00Z", currentMonth.year, currentMonth.month))
        )

        runTest {
            transactionRepository.insertTransaction(incomeTx)
            transactionRepository.insertTransaction(expenseTx)
            budgetRepository.setMonthlyBudget(
                MonthlyBudget(
                    yearMonth = currentMonth,
                    limitAmount = 300000L
                )
            )
            categoryBudgetRepository.setCategoryBudget(
                CategoryBudget(
                    yearMonth = currentMonth,
                    category = TransactionCategory.GROCERIES,
                    limitAmount = 200000L
                )
            )
        }

        val viewModel = TransactionListViewModel(
            getTransactionsByMonthUseCase = GetTransactionsByMonthUseCase(transactionRepository),
            getCashFlowSummaryUseCase = org.hoods.pocket.domain.usecase.GetCashFlowSummaryUseCase(transactionRepository),
            getMonthlyBudgetSummaryUseCase = org.hoods.pocket.domain.usecase.GetMonthlyBudgetSummaryUseCase(budgetRepository, transactionRepository),
            getCategoryBudgetSummariesUseCase = org.hoods.pocket.domain.usecase.GetCategoryBudgetSummariesUseCase(categoryBudgetRepository, transactionRepository),
            initialMonth = currentMonth
        )

        setContent {
            TransactionListScreen(
                viewModel = viewModel,
                onAddTransactionClick = {},
                onTransactionClick = {}
            )
        }

        waitForIdle()

        // Cash flow overview assertions
        onNodeWithTag("cash_flow_overview").assertIsDisplayed()
        onNodeWithTag("net_balance_amount").assertIsDisplayed()
        onNodeWithTag("income_amount").assertIsDisplayed()
        onNodeWithTag("spent_amount").assertIsDisplayed()

        // Budget health & category budget assertions
        onNodeWithTag("budget_health_overview").assertIsDisplayed()
        onNodeWithTag("total_budget_spent_amount").assertIsDisplayed()
        onNodeWithTag("category_budgets_overview").assertIsDisplayed()
        onNodeWithTag("category_budget_card_${TransactionCategory.GROCERIES.name}").assertIsDisplayed()
    }

    @Test
    fun cashFlowAndBudgetHealth_whenNoBudgetSet_displaysEmptyBudgetState() = runComposeUiTest {
        val currentMonth = YearMonth.now()
        val transactionRepository = FakeTransactionRepository()
        val budgetRepository = FakeBudgetRepository()
        val categoryBudgetRepository = FakeCategoryBudgetRepository()

        val viewModel = TransactionListViewModel(
            getTransactionsByMonthUseCase = GetTransactionsByMonthUseCase(transactionRepository),
            getCashFlowSummaryUseCase = org.hoods.pocket.domain.usecase.GetCashFlowSummaryUseCase(transactionRepository),
            getMonthlyBudgetSummaryUseCase = org.hoods.pocket.domain.usecase.GetMonthlyBudgetSummaryUseCase(budgetRepository, transactionRepository),
            getCategoryBudgetSummariesUseCase = org.hoods.pocket.domain.usecase.GetCategoryBudgetSummariesUseCase(categoryBudgetRepository, transactionRepository),
            initialMonth = currentMonth
        )

        setContent {
            TransactionListScreen(
                viewModel = viewModel,
                onAddTransactionClick = {},
                onTransactionClick = {}
            )
        }

        waitForIdle()

        // Cash flow overview is still present showing zero state
        onNodeWithTag("cash_flow_overview").assertIsDisplayed()
        onNodeWithTag("net_balance_amount").assertIsDisplayed()

        // Budget health overview is displayed indicating no budget is set
        onNodeWithTag("budget_health_overview").assertIsDisplayed()
        onNodeWithText("No budget set for ${currentMonth.format()}").assertIsDisplayed()
    }
}
