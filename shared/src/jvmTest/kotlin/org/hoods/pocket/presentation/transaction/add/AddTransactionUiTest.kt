package org.hoods.pocket.presentation.transaction.add

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.domain.usecase.AddTransactionUseCase
import org.hoods.pocket.domain.usecase.FakeTransactionRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class AddTransactionUiTest {

    @Test
    fun validAdd_savesTransactionAndTriggersCompletion() = runComposeUiTest {
        val repository = FakeTransactionRepository()
        val useCase = AddTransactionUseCase(repository)
        var saved = false

        setContent {
            val viewModel = AddTransactionViewModel(useCase)
            AddTransactionScreen(
                viewModel = viewModel,
                onSaveSuccess = { saved = true },
                onDismiss = {}
            )
        }

        onNodeWithTag("description_input").performTextInput("Coffee & Bagel")
        onNodeWithTag("amount_input").performTextInput("12.50")
        onNodeWithTag("save_transaction_button").performClick()

        waitForIdle()

        assertTrue(saved)
        runTest {
            val transactions = repository.getTransactions().first()
            assertEquals(1, transactions.size)
            assertEquals("Coffee & Bagel", transactions.first().description)
            assertEquals(1250L, transactions.first().amount)
            assertEquals(TransactionType.EXPENSE, transactions.first().type)
        }
    }

    @Test
    fun invalidInput_displaysErrorAndPreventsSave_whenDescriptionIsBlank() = runComposeUiTest {
        val repository = FakeTransactionRepository()
        val useCase = AddTransactionUseCase(repository)
        var saved = false

        setContent {
            val viewModel = AddTransactionViewModel(useCase)
            AddTransactionScreen(
                viewModel = viewModel,
                onSaveSuccess = { saved = true },
                onDismiss = {}
            )
        }

        onNodeWithTag("amount_input").performTextInput("12.50")
        onNodeWithTag("save_transaction_button").performClick()

        waitForIdle()

        onNodeWithTag("error_message").assertIsDisplayed()
        onNodeWithText("Transaction description must not be blank").assertIsDisplayed()
        assertFalse(saved)
        runTest {
            assertTrue(repository.getTransactions().first().isEmpty())
        }
    }

    @Test
    fun invalidInput_displaysErrorAndPreventsSave_whenAmountIsZero() = runComposeUiTest {
        val repository = FakeTransactionRepository()
        val useCase = AddTransactionUseCase(repository)
        var saved = false

        setContent {
            val viewModel = AddTransactionViewModel(useCase)
            AddTransactionScreen(
                viewModel = viewModel,
                onSaveSuccess = { saved = true },
                onDismiss = {}
            )
        }

        onNodeWithTag("description_input").performTextInput("Coffee")
        onNodeWithTag("amount_input").performTextInput("0")
        onNodeWithTag("save_transaction_button").performClick()

        waitForIdle()

        onNodeWithTag("error_message").assertIsDisplayed()
        onNodeWithText("Please enter a valid amount greater than zero").assertIsDisplayed()
        assertFalse(saved)
        runTest {
            assertTrue(repository.getTransactions().first().isEmpty())
        }
    }
}
