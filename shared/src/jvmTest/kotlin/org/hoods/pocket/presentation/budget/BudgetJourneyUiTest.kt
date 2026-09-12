package org.hoods.pocket.presentation.budget

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import org.hoods.pocket.PocketBudgetRoot
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.usecase.FakeBudgetRepository
import org.hoods.pocket.domain.usecase.FakeTransactionRepository
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class BudgetJourneyUiTest {

    @Test
    fun completeBudgetJourney_crossScreenSetAndObserveWithTransactions() = runComposeUiTest {
        val transactionRepository = FakeTransactionRepository()
        val budgetRepository = FakeBudgetRepository()
        val currentMonth = YearMonth.now()
        val prevMonth = currentMonth.previous()

        setContent {
            PocketBudgetRoot(
                repository = transactionRepository,
                budgetRepository = budgetRepository
            )
        }

        waitForIdle()

        // 1. Initially on Transactions List screen
        onNodeWithText("Transactions").assertIsDisplayed()

        // 2. Navigate to Budgets tab
        onNodeWithTag("nav_budgets").performClick()
        waitForIdle()

        // 3. Confirm empty budget state for current month
        onNodeWithText("No budget set for ${currentMonth.format()}", substring = true).assertIsDisplayed()
        onNodeWithTag("set_monthly_budget_button").assertIsDisplayed()

        // 4. Click Set Monthly Budget
        onNodeWithTag("set_monthly_budget_button").performClick()
        waitForIdle()

        // 5. On Set Budget screen: enter $2,000.00 and save
        onNodeWithText("Set ${currentMonth.format()} Budget").assertIsDisplayed()
        onNodeWithTag("budget_amount_input").performTextInput("2000.00")
        onNodeWithTag("save_budget_button").performClick()
        waitForIdle()

        // 6. Returned to Budget Dashboard: shows $0.00 / $2,000.00
        onNodeWithText("Total Budget Used").assertIsDisplayed()
        onNodeWithText("$0.00", substring = true).assertIsDisplayed()
        onNodeWithText("/ $2000.00", substring = true).assertIsDisplayed()
        onNodeWithText("$2000.00 left").assertIsDisplayed()

        // 7. Navigate back to Transactions tab
        onNodeWithTag("nav_transactions").performClick()
        waitForIdle()

        // 8. Add an Expense of $150.00
        onNodeWithTag("add_transaction_fab").performClick()
        waitForIdle()
        onNodeWithTag("description_input").performTextInput("Groceries")
        onNodeWithTag("amount_input").performTextInput("150.00")
        onNodeWithTag("save_transaction_button").performClick()
        waitForIdle()

        // 9. Add an Income of $500.00 (Income MUST NOT count as spending)
        onNodeWithTag("add_transaction_fab").performClick()
        waitForIdle()
        onNodeWithTag("type_toggle_income").performClick()
        onNodeWithTag("description_input").performTextInput("Bonus")
        onNodeWithTag("amount_input").performTextInput("500.00")
        onNodeWithTag("save_transaction_button").performClick()
        waitForIdle()

        // 10. Return to Budgets tab
        onNodeWithTag("nav_budgets").performClick()
        waitForIdle()

        // 11. Verify dashboard shows spent $150.00 (income ignored!) and $1,850.00 left
        onNodeWithText("$150.00", substring = true).assertIsDisplayed()
        onNodeWithText("/ $2000.00", substring = true).assertIsDisplayed()
        onNodeWithText("$1850.00 left").assertIsDisplayed()

        // 12. Month browsing: go to previous month (empty state)
        onNodeWithTag("previous_month_button").performClick()
        waitForIdle()
        onNodeWithTag("selected_month_text").assertTextEquals(prevMonth.format())
        onNodeWithTag("set_monthly_budget_button").assertIsDisplayed()

        // 13. Return to current month via Today button
        onNodeWithTag("current_month_button").performClick()
        waitForIdle()
        onNodeWithText("$150.00", substring = true).assertIsDisplayed()
        onNodeWithText("$1850.00 left").assertIsDisplayed()
    }

    @Test
    fun completeBudgetLifecycleJourney_setEditCancelDeleteConfirm() = runComposeUiTest {
        val transactionRepository = FakeTransactionRepository()
        val budgetRepository = FakeBudgetRepository()
        val currentMonth = YearMonth.now()

        setContent {
            PocketBudgetRoot(
                repository = transactionRepository,
                budgetRepository = budgetRepository
            )
        }

        waitForIdle()

        // 1. Navigate to Budgets tab (empty state)
        onNodeWithTag("nav_budgets").performClick()
        waitForIdle()

        onNodeWithText("No budget set for ${currentMonth.format()}", substring = true).assertIsDisplayed()
        onNodeWithTag("set_monthly_budget_button").assertIsDisplayed()

        // 2. Click Set Monthly Budget
        onNodeWithTag("set_monthly_budget_button").performClick()
        waitForIdle()

        // 3. Enter $3,000.00 and save
        onNodeWithText("Set ${currentMonth.format()} Budget").assertIsDisplayed()
        onNodeWithTag("budget_amount_input").performTextInput("3000.00")
        onNodeWithTag("save_budget_button").performClick()
        waitForIdle()

        // 4. Return to Budget Dashboard: displays / $3000.00
        onNodeWithText("Total Budget Used").assertIsDisplayed()
        onNodeWithText("/ $3000.00", substring = true).assertIsDisplayed()
        onNodeWithTag("edit_budget_button").assertIsDisplayed()

        // 5. Click edit button -> navigate to SetBudget with prefilled values
        onNodeWithTag("edit_budget_button").performClick()
        waitForIdle()

        // 6. Verify prefilled amount and delete button visible
        onNodeWithTag("delete_budget_button").assertIsDisplayed()
        onNodeWithText("Save Changes").assertIsDisplayed()

        // 7. Edit amount to $4,500.00 and save
        onNodeWithTag("budget_amount_input").performTextClearance()
        onNodeWithTag("budget_amount_input").performTextInput("4500.00")
        onNodeWithTag("save_budget_button").performClick()
        waitForIdle()

        // 8. Return to Budget Dashboard: verify updated limit / $4500.00
        onNodeWithText("/ $4500.00", substring = true).assertIsDisplayed()

        // 9. Click edit button again
        onNodeWithTag("edit_budget_button").performClick()
        waitForIdle()

        // 10. Click delete button -> confirmation dialog appears
        onNodeWithTag("delete_budget_button").performClick()
        waitForIdle()
        onNodeWithText("Delete Monthly Budget?").assertIsDisplayed()

        // 11. Click cancel -> dialog dismisses, budget remains
        onNodeWithTag("cancel_delete_button").performClick()
        waitForIdle()
        onNodeWithTag("budget_amount_input").assertIsDisplayed()

        // 12. Click delete again -> confirm delete
        onNodeWithTag("delete_budget_button").performClick()
        waitForIdle()
        onNodeWithTag("confirm_delete_button").performClick()
        waitForIdle()

        // 13. Returned to Budget Dashboard: empty state is restored
        onNodeWithText("No budget set for ${currentMonth.format()}", substring = true).assertIsDisplayed()
        onNodeWithTag("set_monthly_budget_button").assertIsDisplayed()
    }
}
