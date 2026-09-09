package org.hoods.pocket.presentation.transaction.edit

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
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.domain.usecase.DeleteTransactionUseCase
import org.hoods.pocket.domain.usecase.FakeTransactionRepository
import org.hoods.pocket.domain.usecase.GetTransactionByIdUseCase
import org.hoods.pocket.domain.usecase.UpdateTransactionUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class EditTransactionUiTest {

    @Test
    fun editWithoutDuplication_updatesExistingRecordAndRetainsCount() = runComposeUiTest {
        val repository = FakeTransactionRepository()
        val original = Transaction(
            id = "tx-100",
            description = "Dinner",
            amount = 3500L,
            type = TransactionType.EXPENSE,
            date = Instant.fromEpochMilliseconds(1717171717000L)
        )
        runTest { repository.insertTransaction(original) }

        val viewModel = EditTransactionViewModel(
            getTransactionByIdUseCase = GetTransactionByIdUseCase(repository),
            updateTransactionUseCase = UpdateTransactionUseCase(repository),
            deleteTransactionUseCase = DeleteTransactionUseCase(repository)
        )
        var saved = false

        setContent {
            EditTransactionScreen(
                viewModel = viewModel,
                transactionId = "tx-100",
                onSaveSuccess = { saved = true },
                onDeleteSuccess = {},
                onDismiss = {}
            )
        }

        waitForIdle()

        // Verify pre-filled data is visible
        onNodeWithTag("description_input").assertIsDisplayed()
        onNodeWithTag("description_input").performTextClearance()
        onNodeWithTag("description_input").performTextInput("Supermarket Dinner")

        onNodeWithTag("amount_input").performTextClearance()
        onNodeWithTag("amount_input").performTextInput("42.00")

        onNodeWithTag("save_changes_button").performClick()
        waitForIdle()

        assertTrue(saved)
        runTest {
            val transactions = repository.getTransactions().first()
            assertEquals(1, transactions.size)
            val updated = transactions.first()
            assertEquals("tx-100", updated.id)
            assertEquals("Supermarket Dinner", updated.description)
            assertEquals(4200L, updated.amount)
            assertEquals(TransactionType.EXPENSE, updated.type)
            assertEquals(original.date, updated.date)
        }
    }

    @Test
    fun cancelDeletion_leavesTransactionIntactInRepository() = runComposeUiTest {
        val repository = FakeTransactionRepository()
        val original = Transaction(
            id = "tx-100",
            description = "Dinner",
            amount = 3500L,
            type = TransactionType.EXPENSE,
            date = Instant.fromEpochMilliseconds(1717171717000L)
        )
        runTest { repository.insertTransaction(original) }

        val viewModel = EditTransactionViewModel(
            getTransactionByIdUseCase = GetTransactionByIdUseCase(repository),
            updateTransactionUseCase = UpdateTransactionUseCase(repository),
            deleteTransactionUseCase = DeleteTransactionUseCase(repository)
        )

        setContent {
            EditTransactionScreen(
                viewModel = viewModel,
                transactionId = "tx-100",
                onSaveSuccess = {},
                onDeleteSuccess = {},
                onDismiss = {}
            )
        }

        waitForIdle()

        // Trigger delete
        onNodeWithTag("delete_transaction_button").performClick()
        waitForIdle()

        // Confirm dialog is displayed
        onNodeWithText("Delete Transaction?").assertIsDisplayed()
        onNodeWithTag("cancel_delete_button").assertIsDisplayed()

        // Cancel deletion
        onNodeWithTag("cancel_delete_button").performClick()
        waitForIdle()

        // Confirm dialog dismissed and transaction remains in repository
        onNodeWithTag("confirm_delete_button").assertDoesNotExist()
        runTest {
            val transactions = repository.getTransactions().first()
            assertEquals(1, transactions.size)
            assertEquals("tx-100", transactions.first().id)
        }
    }

    @Test
    fun confirmedDeletion_removesTransactionAndTriggersDeleteCallback() = runComposeUiTest {
        val repository = FakeTransactionRepository()
        val original = Transaction(
            id = "tx-100",
            description = "Dinner",
            amount = 3500L,
            type = TransactionType.EXPENSE,
            date = Instant.fromEpochMilliseconds(1717171717000L)
        )
        runTest { repository.insertTransaction(original) }

        val viewModel = EditTransactionViewModel(
            getTransactionByIdUseCase = GetTransactionByIdUseCase(repository),
            updateTransactionUseCase = UpdateTransactionUseCase(repository),
            deleteTransactionUseCase = DeleteTransactionUseCase(repository)
        )
        var deleted = false

        setContent {
            EditTransactionScreen(
                viewModel = viewModel,
                transactionId = "tx-100",
                onSaveSuccess = {},
                onDeleteSuccess = { deleted = true },
                onDismiss = {}
            )
        }

        waitForIdle()

        // Trigger delete
        onNodeWithTag("delete_transaction_button").performClick()
        waitForIdle()

        // Confirm dialog is displayed
        onNodeWithText("Delete Transaction?").assertIsDisplayed()
        onNodeWithTag("confirm_delete_button").performClick()
        waitForIdle()

        assertTrue(deleted)
        runTest {
            val transactions = repository.getTransactions().first()
            assertTrue(transactions.isEmpty())
        }
    }
}
