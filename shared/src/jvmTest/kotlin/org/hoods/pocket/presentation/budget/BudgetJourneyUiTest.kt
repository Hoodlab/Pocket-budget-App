package org.hoods.pocket.presentation.budget

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
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
import org.hoods.pocket.domain.usecase.FakeCategoryBudgetRepository
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

    @Test
    fun completeCategoryBudgetJourney_setAllocationsAndVerifyPersistenceAndReset() = runComposeUiTest {
        val transactionRepository = FakeTransactionRepository()
        val budgetRepository = FakeBudgetRepository()
        val categoryBudgetRepository = FakeCategoryBudgetRepository()
        val currentMonth = YearMonth.now()

        setContent {
            PocketBudgetRoot(
                repository = transactionRepository,
                budgetRepository = budgetRepository,
                categoryBudgetRepository = categoryBudgetRepository
            )
        }

        waitForIdle()

        // 1. Navigate to Budgets tab (empty state)
        onNodeWithTag("nav_budgets").performClick()
        waitForIdle()

        // 2. Click Set Monthly Budget
        onNodeWithTag("set_monthly_budget_button").performClick()
        waitForIdle()

        // 3. Enter Monthly Budget of $2,000.00
        onNodeWithTag("budget_amount_input").performTextInput("2000.00")
        waitForIdle()

        // Verify remaining to allocate initially shows $2000.00
        onNodeWithTag("remaining_allocation_text").assertTextEquals("Remaining to Allocate: $2000.00")

        // 4. Enter valid category allocations: Groceries $500.00, Dining Out $300.00, Transport $200.00
        onNodeWithTag("category_allocation_input_groceries").performTextInput("500.00")
        onNodeWithTag("category_allocation_input_dining").performTextInput("300.00")
        onNodeWithTag("category_allocation_input_transport").performTextInput("200.00")
        waitForIdle()

        // Remaining to allocate should be $1000.00
        onNodeWithTag("remaining_allocation_text").assertTextEquals("Remaining to Allocate: $1000.00")

        // 5. Over-allocation check: enter Shopping $1200.00 ($2000 - $500 - $300 - $200 - $1200 = -$200.00)
        onNodeWithTag("category_allocation_input_shopping").performTextInput("1200.00")
        waitForIdle()

        onNodeWithTag("remaining_allocation_text").assertTextEquals("Over allocated by $200.00")
        onNodeWithTag("save_budget_button").performClick()
        waitForIdle()
        onNodeWithTag("error_message_text").assertIsDisplayed()

        // 6. Correct Shopping to $200.00
        onNodeWithTag("category_allocation_input_shopping").performTextClearance()
        onNodeWithTag("category_allocation_input_shopping").performTextInput("200.00")
        waitForIdle()

        onNodeWithTag("remaining_allocation_text").assertTextEquals("Remaining to Allocate: $800.00")

        // 7. Save the budget with category allocations
        onNodeWithTag("save_budget_button").performClick()
        waitForIdle()

        // 8. We are back on dashboard: click edit to reopen SetBudget
        onNodeWithTag("edit_budget_button").performClick()
        waitForIdle()

        // 9. Verify prefilled allocations persisted
        onNodeWithTag("category_allocation_input_groceries").assertTextEquals("$ ", "500.00")
        onNodeWithTag("category_allocation_input_dining").assertTextEquals("$ ", "300.00")
        onNodeWithTag("category_allocation_input_transport").assertTextEquals("$ ", "200.00")
        onNodeWithTag("category_allocation_input_shopping").assertTextEquals("$ ", "200.00")
        onNodeWithTag("remaining_allocation_text").assertTextEquals("Remaining to Allocate: $800.00")

        // 10. Click Reset Allocations button
        onNodeWithTag("reset_allocations_button").performClick()
        waitForIdle()

        // All category allocation inputs should be cleared and remaining back to full total
        onNodeWithTag("remaining_allocation_text").assertTextEquals("Remaining to Allocate: $2000.00")

        // 11. Save the reset budget
        onNodeWithTag("save_budget_button").performClick()
        waitForIdle()

        // Back to dashboard
        onNodeWithText("Total Budget Used").assertIsDisplayed()
    }

    @Test
    fun categoryBudgetHealthJourney_expenseAddChangeDelete_incomeIgnored_uncategorizedIsolated() = runComposeUiTest {
        val transactionRepository = FakeTransactionRepository()
        val budgetRepository = FakeBudgetRepository()
        val categoryBudgetRepository = FakeCategoryBudgetRepository()
        val currentMonth = YearMonth.now()
        val prevMonth = currentMonth.previous()

        setContent {
            PocketBudgetRoot(
                repository = transactionRepository,
                budgetRepository = budgetRepository,
                categoryBudgetRepository = categoryBudgetRepository
            )
        }

        waitForIdle()

        // 1. Navigate to Budgets tab
        onNodeWithTag("nav_budgets").performClick()
        waitForIdle()

        // 2. Click Set Monthly Budget
        onNodeWithTag("set_monthly_budget_button").performClick()
        waitForIdle()

        // 3. Enter Monthly Budget $1,000.00 and allocations: Groceries $400.00, Dining Out $200.00
        onNodeWithTag("budget_amount_input").performTextInput("1000.00")
        onNodeWithTag("category_allocation_input_groceries").performTextInput("400.00")
        onNodeWithTag("category_allocation_input_dining").performTextInput("200.00")
        onNodeWithTag("save_budget_button").performClick()
        waitForIdle()

        // 4. On dashboard: verify Category Budgets header and category cards
        onNodeWithTag("category_budgets_title").assertIsDisplayed()
        onNodeWithTag("category_budget_card_GROCERIES").assertIsDisplayed()
        onNodeWithTag("category_budget_card_DINING").assertIsDisplayed()
        onNodeWithTag("category_remaining_GROCERIES").assertTextContains("400.00", substring = true)
        onNodeWithTag("category_remaining_DINING").assertTextContains("200.00", substring = true)

        // 5. Navigate to Transactions tab and add Groceries expense: $320.00 (80% of $400 -> Near Limit)
        onNodeWithTag("nav_transactions").performClick()
        waitForIdle()
        onNodeWithTag("add_transaction_fab").performClick()
        waitForIdle()
        onNodeWithTag("description_input").performTextInput("Weekly groceries")
        onNodeWithTag("amount_input").performTextInput("320.00")
        onNodeWithTag("category_chip_groceries").performClick()
        onNodeWithTag("save_transaction_button").performClick()
        waitForIdle()

        // 6. Return to Budgets tab: verify Groceries is Near Limit ($80.00 left)
        onNodeWithTag("nav_budgets").performClick()
        waitForIdle()
        onNodeWithTag("category_remaining_GROCERIES").assertTextContains("80.00", substring = true)
        onNodeWithTag("category_health_badge_GROCERIES").assertIsDisplayed()
        onNodeWithText("Near Limit", substring = true).assertIsDisplayed()

        // 7. Add Income transaction of $500.00 (MUST NOT change category health)
        onNodeWithTag("nav_transactions").performClick()
        waitForIdle()
        onNodeWithTag("add_transaction_fab").performClick()
        waitForIdle()
        onNodeWithTag("type_toggle_income").performClick()
        onNodeWithTag("description_input").performTextInput("Paycheck")
        onNodeWithTag("amount_input").performTextInput("500.00")
        onNodeWithTag("category_chip_salary").performClick()
        onNodeWithTag("save_transaction_button").performClick()
        waitForIdle()

        onNodeWithTag("nav_budgets").performClick()
        waitForIdle()
        // Groceries still $80.00 left, Near Limit
        onNodeWithTag("category_remaining_GROCERIES").assertTextContains("80.00", substring = true)

        // 8. Add Uncategorized expense of $50.00 (updates overall total, but no Uncategorized category card)
        onNodeWithTag("nav_transactions").performClick()
        waitForIdle()
        onNodeWithTag("add_transaction_fab").performClick()
        waitForIdle()
        onNodeWithTag("description_input").performTextInput("Random item")
        onNodeWithTag("amount_input").performTextInput("50.00")
        // Default category is UNCATEGORIZED
        onNodeWithTag("save_transaction_button").performClick()
        waitForIdle()

        onNodeWithTag("nav_budgets").performClick()
        waitForIdle()
        // Overall spent is $370.00 ($320 + $50)
        onNodeWithText("370.00", substring = true).assertIsDisplayed()
        // Groceries spent is still $320.00 ($80.00 left)
        onNodeWithTag("category_remaining_GROCERIES").assertTextContains("80.00", substring = true)

        // 9. Recategorise: Edit Groceries transaction to Dining ($320.00 on $200.00 limit -> Dining Over Budget)
        onNodeWithTag("nav_transactions").performClick()
        waitForIdle()
        onNodeWithText("Weekly groceries").performClick()
        waitForIdle()
        onNodeWithTag("category_chip_dining").performClick()
        onNodeWithTag("save_changes_button").performClick()
        waitForIdle()

        onNodeWithTag("nav_budgets").performClick()
        waitForIdle()
        // Groceries back to $400.00 left
        onNodeWithTag("category_remaining_GROCERIES").assertTextContains("400.00", substring = true)
        // Dining is now $320 on $200 limit -> Over Budget by $120.00
        onNodeWithTag("category_remaining_DINING").assertTextContains("120.00", substring = true)
        onNodeWithTag("category_health_badge_DINING").assertIsDisplayed()
        onNodeWithText("Over Budget", substring = true).assertIsDisplayed()

        // 10. Delete the Dining transaction
        onNodeWithTag("nav_transactions").performClick()
        waitForIdle()
        onNodeWithText("Weekly groceries").performClick()
        waitForIdle()
        onNodeWithTag("delete_transaction_button").performClick()
        waitForIdle()
        onNodeWithTag("confirm_delete_button").performClick()
        waitForIdle()

        onNodeWithTag("nav_budgets").performClick()
        waitForIdle()
        // Dining restored to full $200.00 left
        onNodeWithTag("category_remaining_DINING").assertTextContains("200.00", substring = true)

        // 11. Month isolation: go to previous month
        onNodeWithTag("previous_month_button").performClick()
        waitForIdle()
        onNodeWithText("No budget set for ${prevMonth.format()}", substring = true).assertIsDisplayed()
    }
}
