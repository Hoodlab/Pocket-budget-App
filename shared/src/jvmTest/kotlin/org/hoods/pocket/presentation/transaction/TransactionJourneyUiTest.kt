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
import org.hoods.pocket.domain.usecase.FakeTransactionRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TransactionJourneyUiTest {

    @Test
    fun completeTransactionLifecycleJourney_addEditDeleteCrossScreen() = runComposeUiTest {
        val repository = FakeTransactionRepository()

        setContent {
            PocketBudgetRoot(repository = repository)
        }

        waitForIdle()

        // 1. Initial State: Transaction List renders empty state
        onNodeWithText("Transactions").assertIsDisplayed()
        onNodeWithText("No transactions yet\nTap + to add one").assertIsDisplayed()

        // 2. Navigate: Tap FAB to open Add Transaction form
        onNodeWithTag("add_transaction_fab").performClick()
        waitForIdle()

        // 3. Add Screen: Fill valid transaction details and submit
        onNodeWithTag("description_input").assertIsDisplayed()
        onNodeWithTag("description_input").performTextInput("Groceries")
        onNodeWithTag("amount_input").performTextInput("45.50")
        onNodeWithTag("save_transaction_button").performClick()
        waitForIdle()

        // 4. Return to List: Verify added transaction appears
        onNodeWithText("Groceries").assertIsDisplayed()
        onNodeWithText("-$45.50").assertIsDisplayed()

        var txId = ""
        runTest {
            val list = repository.getTransactions().first()
            assertEquals(1, list.size)
            txId = list.first().id
        }

        // 5. Navigate to Edit: Tap the transaction item
        onNodeWithTag("transaction_item_$txId").performClick()
        waitForIdle()

        // 6. Edit Screen: Verify pre-filled data, modify values, and save
        onNodeWithTag("description_input").assertIsDisplayed()
        onNodeWithTag("description_input").performTextClearance()
        onNodeWithTag("description_input").performTextInput("Weekly Groceries")

        onNodeWithTag("amount_input").performTextClearance()
        onNodeWithTag("amount_input").performTextInput("52.00")

        onNodeWithTag("save_changes_button").performClick()
        waitForIdle()

        // 7. Return to List: Verify updated description and amount, without duplicate records
        onNodeWithText("Weekly Groceries").assertIsDisplayed()
        onNodeWithText("-$52.00").assertIsDisplayed()
        runTest {
            val list = repository.getTransactions().first()
            assertEquals(1, list.size)
            assertEquals("Weekly Groceries", list.first().description)
            assertEquals(5200L, list.first().amount)
        }

        // 8. Open Edit again for Deletion
        onNodeWithTag("transaction_item_$txId").performClick()
        waitForIdle()

        // 9. Cancel Deletion Dialog: Verify transaction remains intact
        onNodeWithTag("delete_transaction_button").performClick()
        waitForIdle()
        onNodeWithText("Delete Transaction?").assertIsDisplayed()
        onNodeWithTag("cancel_delete_button").performClick()
        waitForIdle()
        onNodeWithTag("confirm_delete_button").assertDoesNotExist()

        // 10. Confirm Deletion: Verify transaction is removed and returns to empty list
        onNodeWithTag("delete_transaction_button").performClick()
        waitForIdle()
        onNodeWithTag("confirm_delete_button").performClick()
        waitForIdle()

        // 11. Final List State: Verify empty state is rendered and repository is empty
        onNodeWithText("No transactions yet\nTap + to add one").assertIsDisplayed()
        runTest {
            val list = repository.getTransactions().first()
            assertTrue(list.isEmpty())
        }
    }
}
