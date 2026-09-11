package org.hoods.pocket.presentation.budget.set

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.usecase.FakeBudgetRepository
import org.hoods.pocket.domain.usecase.SetMonthlyBudgetUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SetBudgetUiTest {

    @Test
    fun validInput_savesBudgetAndInvokesOnSaveSuccess() = runComposeUiTest {
        val repository = FakeBudgetRepository()
        val useCase = SetMonthlyBudgetUseCase(repository)
        val targetMonth = YearMonth(2026, 8)
        var saved = false

        setContent {
            val viewModel = SetBudgetViewModel(useCase, targetMonth)
            SetBudgetScreen(
                viewModel = viewModel,
                onSaveSuccess = { saved = true },
                onDismiss = {}
            )
        }

        waitForIdle()

        onNodeWithText("Set August 2026 Budget").assertIsDisplayed()
        onNodeWithTag("budget_amount_input").performTextInput("2500.00")
        onNodeWithTag("save_budget_button").performClick()

        waitForIdle()

        assertTrue(saved)
        runTest {
            val budget = repository.getMonthlyBudget(targetMonth).first()
            assertNotNull(budget)
            assertEquals(250000L, budget.limitAmount)
        }
    }

    @Test
    fun invalidInput_displaysErrorAndPreventsSave() = runComposeUiTest {
        val repository = FakeBudgetRepository()
        val useCase = SetMonthlyBudgetUseCase(repository)
        val targetMonth = YearMonth(2026, 8)
        var saved = false

        setContent {
            val viewModel = SetBudgetViewModel(useCase, targetMonth)
            SetBudgetScreen(
                viewModel = viewModel,
                onSaveSuccess = { saved = true },
                onDismiss = {}
            )
        }

        waitForIdle()

        // 1. Submit blank
        onNodeWithTag("save_budget_button").performClick()
        waitForIdle()

        onNodeWithTag("error_message_text").assertIsDisplayed()
        onNodeWithText("Budget amount must not be blank").assertIsDisplayed()
        assertFalse(saved)

        // 2. Submit 0
        onNodeWithTag("budget_amount_input").performTextInput("0")
        onNodeWithTag("save_budget_button").performClick()
        waitForIdle()

        onNodeWithTag("error_message_text").assertIsDisplayed()
        onNodeWithText("Please enter a valid amount greater than zero").assertIsDisplayed()
        assertFalse(saved)

        runTest {
            assertNull(repository.getMonthlyBudget(targetMonth).first())
        }
    }

    @Test
    fun dismissAction_triggersOnDismiss() = runComposeUiTest {
        val repository = FakeBudgetRepository()
        val useCase = SetMonthlyBudgetUseCase(repository)
        val targetMonth = YearMonth(2026, 8)
        var dismissed = false

        setContent {
            val viewModel = SetBudgetViewModel(useCase, targetMonth)
            SetBudgetScreen(
                viewModel = viewModel,
                onSaveSuccess = {},
                onDismiss = { dismissed = true }
            )
        }

        waitForIdle()

        onNodeWithTag("back_button").performClick()
        waitForIdle()

        assertTrue(dismissed)
    }
}
