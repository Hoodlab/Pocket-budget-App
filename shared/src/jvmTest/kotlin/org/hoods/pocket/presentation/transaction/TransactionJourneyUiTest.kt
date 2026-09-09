package org.hoods.pocket.presentation.transaction

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.PocketBudgetRoot
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.usecase.FakeTransactionRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TransactionJourneyUiTest {

    @Test
    fun completeTransactionLifecycleJourney_addEditDeleteCrossScreen() = runComposeUiTest {
        val repository = FakeTransactionRepository()
        val currentMonth = YearMonth.now()
        val prevMonth = currentMonth.previous()

        setContent {
            PocketBudgetRoot(repository = repository)
        }

        waitForIdle()

        // 1. Initial State: Transaction List renders month header and empty state
        onNodeWithText("Transactions").assertIsDisplayed()
        onNodeWithTag("selected_month_text").assertIsDisplayed()
        onNodeWithText(currentMonth.format()).assertIsDisplayed()
        onNodeWithText("No transactions yet\nTap + to add one").assertIsDisplayed()

        // 2. Navigate: Tap FAB to open Add Transaction form
        onNodeWithTag("add_transaction_fab").performClick()
        waitForIdle()

        // 3. Add Screen: Default date is Today; fill details and submit
        onNodeWithTag("date_picker_button").assertIsDisplayed()
        onNodeWithText("Today", substring = true).assertIsDisplayed()
        onNodeWithTag("description_input").assertIsDisplayed()
        onNodeWithTag("description_input").performTextInput("Groceries")
        onNodeWithTag("amount_input").performTextInput("45.50")
        onNodeWithTag("save_transaction_button").performClick()
        waitForIdle()

        // 4. Return to List: Verify added transaction appears in current month
        onNodeWithText("Groceries").assertIsDisplayed()
        onNodeWithText("-$45.50").assertIsDisplayed()

        // 5. Month Browsing: Navigate to previous month, verify filtering, and return to current
        onNodeWithTag("previous_month_button").performClick()
        waitForIdle()
        onNodeWithText(prevMonth.format()).assertIsDisplayed()
        onNodeWithText("Groceries").assertDoesNotExist()
        onNodeWithTag("current_month_button").assertIsDisplayed()

        onNodeWithTag("current_month_button").performClick()
        waitForIdle()
        onNodeWithText(currentMonth.format()).assertIsDisplayed()
        onNodeWithText("Groceries").assertIsDisplayed()

        var txId = ""
        runTest {
            val list = repository.getTransactions().first()
            assertEquals(1, list.size)
            txId = list.first().id
        }

        // 6. Navigate to Edit: Tap the transaction item
        onNodeWithTag("transaction_item_$txId").performClick()
        waitForIdle()

        // 7. Edit Screen: Verify pre-filled data & stored date, modify values, and save
        onNodeWithTag("date_picker_button").assertIsDisplayed()
        onNodeWithTag("description_input").assertIsDisplayed()
        onNodeWithTag("description_input").performTextClearance()
        onNodeWithTag("description_input").performTextInput("Weekly Groceries")

        onNodeWithTag("amount_input").performTextClearance()
        onNodeWithTag("amount_input").performTextInput("52.00")

        onNodeWithTag("save_changes_button").performClick()
        waitForIdle()

        // 8. Return to List: Verify updated description and amount, without duplicate records
        onNodeWithText("Weekly Groceries").assertIsDisplayed()
        onNodeWithText("-$52.00").assertIsDisplayed()
        runTest {
            val list = repository.getTransactions().first()
            assertEquals(1, list.size)
            assertEquals("Weekly Groceries", list.first().description)
            assertEquals(5200L, list.first().amount)
        }

        // 9. Open Edit again for Deletion
        onNodeWithTag("transaction_item_$txId").performClick()
        waitForIdle()

        // 10. Cancel Deletion Dialog: Verify transaction remains intact
        onNodeWithTag("delete_transaction_button").performClick()
        waitForIdle()
        onNodeWithText("Delete Transaction?").assertIsDisplayed()
        onNodeWithTag("cancel_delete_button").performClick()
        waitForIdle()
        onNodeWithTag("confirm_delete_button").assertDoesNotExist()

        // 11. Confirm Deletion: Verify transaction is removed and returns to empty list
        onNodeWithTag("delete_transaction_button").performClick()
        waitForIdle()
        onNodeWithTag("confirm_delete_button").performClick()
        waitForIdle()

        // 12. Final List State: Verify empty state is rendered and repository is empty
        onNodeWithText("No transactions yet\nTap + to add one").assertIsDisplayed()
        runTest {
            val list = repository.getTransactions().first()
            assertTrue(list.isEmpty())
        }
    }
}
