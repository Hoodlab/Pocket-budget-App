package org.hoods.pocket.presentation.transaction

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.PocketBudgetRoot
import org.hoods.pocket.di.startTestKoin
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.usecase.FakeTransactionRepository
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TransactionJourneyUiTest {

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun completeTransactionLifecycleJourney_addEditDeleteCrossScreen() = runComposeUiTest {
        val repository = FakeTransactionRepository()
        val currentMonth = YearMonth.now()
        val prevMonth = currentMonth.previous()

        startTestKoin(transactionRepository = repository)

        setContent {
            PocketBudgetRoot()
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

        // 3. Add Screen: Default date is Today; select Groceries category, fill details and submit
        onNodeWithTag("date_picker_button").assertIsDisplayed()
        onNodeWithText("Today", substring = true).assertIsDisplayed()
        onNodeWithTag("description_input").assertIsDisplayed()
        onNodeWithTag("description_input").performTextInput("Whole Foods")
        onNodeWithTag("amount_input").performTextInput("45.50")
        onNodeWithTag("category_chip_groceries").performClick()
        onNodeWithTag("save_transaction_button").performClick()
        waitForIdle()

        var txId = ""
        runTest {
            val list = repository.getTransactions().first()
            assertEquals(1, list.size)
            assertEquals(org.hoods.pocket.domain.model.TransactionCategory.GROCERIES, list.first().category)
            txId = list.first().id
        }

        // 4. Return to List: Verify added transaction and category appear in current month
        onNodeWithText("Whole Foods").assertIsDisplayed()
        onNodeWithTag("transaction_item_$txId").assertTextContains("-$45.50")
        onNodeWithText("Groceries", substring = true).assertIsDisplayed()

        // 5. Month Browsing: Navigate to previous month, verify filtering, and return to current
        onNodeWithTag("previous_month_button").performClick()
        waitForIdle()
        onNodeWithText(prevMonth.format()).assertIsDisplayed()
        onNodeWithText("Whole Foods").assertDoesNotExist()
        onNodeWithTag("current_month_button").assertIsDisplayed()

        onNodeWithTag("current_month_button").performClick()
        waitForIdle()
        onNodeWithText(currentMonth.format()).assertIsDisplayed()
        onNodeWithText("Whole Foods").assertIsDisplayed()

        // 6. Navigate to Edit: Tap the transaction item
        onNodeWithTag("transaction_item_$txId").performClick()
        waitForIdle()

        // 7. Edit Screen: Verify pre-filled data & category, change category to Dining, modify values, and save
        onNodeWithTag("date_picker_button").assertIsDisplayed()
        onNodeWithTag("category_chip_groceries").assertIsDisplayed()
        onNodeWithTag("category_chip_dining").performClick()

        onNodeWithTag("description_input").assertIsDisplayed()
        onNodeWithTag("description_input").performTextClearance()
        onNodeWithTag("description_input").performTextInput("Weekly Dining")

        onNodeWithTag("amount_input").performTextClearance()
        onNodeWithTag("amount_input").performTextInput("52.00")

        onNodeWithTag("save_changes_button").performClick()
        waitForIdle()

        // 8. Return to List: Verify updated description, amount, and category, without duplicate records
        onNodeWithText("Weekly Dining").assertIsDisplayed()
        onNodeWithTag("transaction_item_$txId").assertTextContains("-$52.00")
        onNodeWithText("Dining", substring = true).assertIsDisplayed()
        runTest {
            val list = repository.getTransactions().first()
            assertEquals(1, list.size)
            assertEquals("Weekly Dining", list.first().description)
            assertEquals(5200L, list.first().amount)
            assertEquals(org.hoods.pocket.domain.model.TransactionCategory.DINING, list.first().category)
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
