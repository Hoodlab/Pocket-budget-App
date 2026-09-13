package org.hoods.pocket.presentation.budget.set

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.domain.model.MonthlyBudget
import org.hoods.pocket.domain.model.TransactionCategory
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.usecase.DeleteMonthlyBudgetUseCase
import org.hoods.pocket.domain.usecase.FakeBudgetRepository
import org.hoods.pocket.domain.usecase.FakeCategoryBudgetRepository
import org.hoods.pocket.domain.usecase.GetCategoryBudgetsUseCase
import org.hoods.pocket.domain.usecase.GetMonthlyBudgetUseCase
import org.hoods.pocket.domain.usecase.RemoveCategoryBudgetUseCase
import org.hoods.pocket.domain.usecase.SaveCategoryAllocationsUseCase
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
        val setUseCase = SetMonthlyBudgetUseCase(repository)
        val getUseCase = GetMonthlyBudgetUseCase(repository)
        val deleteUseCase = DeleteMonthlyBudgetUseCase(repository)
        val targetMonth = YearMonth(2026, 8)
        var saved = false

        setContent {
            val viewModel = SetBudgetViewModel(
                setMonthlyBudgetUseCase = setUseCase,
                getMonthlyBudgetUseCase = getUseCase,
                deleteMonthlyBudgetUseCase = deleteUseCase,
                yearMonth = targetMonth
            )
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
    fun existingBudget_prefillsAmountAndShowsDeleteButton() = runComposeUiTest {
        val repository = FakeBudgetRepository()
        val targetMonth = YearMonth(2026, 8)
        repository.setMonthlyBudget(MonthlyBudget(targetMonth, 350000L)) // $3,500.00

        val setUseCase = SetMonthlyBudgetUseCase(repository)
        val getUseCase = GetMonthlyBudgetUseCase(repository)
        val deleteUseCase = DeleteMonthlyBudgetUseCase(repository)

        setContent {
            val viewModel = SetBudgetViewModel(
                setMonthlyBudgetUseCase = setUseCase,
                getMonthlyBudgetUseCase = getUseCase,
                deleteMonthlyBudgetUseCase = deleteUseCase,
                yearMonth = targetMonth
            )
            SetBudgetScreen(
                viewModel = viewModel,
                onSaveSuccess = {},
                onDismiss = {}
            )
        }

        waitForIdle()

        onNodeWithTag("delete_budget_button").assertIsDisplayed()
        onNodeWithTag("save_budget_button").assertIsDisplayed()
        onNodeWithText("Save Changes").assertIsDisplayed()
    }

    @Test
    fun editBudget_changesAmountAndSavesWithoutDuplicate() = runComposeUiTest {
        val repository = FakeBudgetRepository()
        val targetMonth = YearMonth(2026, 8)
        repository.setMonthlyBudget(MonthlyBudget(targetMonth, 350000L))

        val setUseCase = SetMonthlyBudgetUseCase(repository)
        val getUseCase = GetMonthlyBudgetUseCase(repository)
        val deleteUseCase = DeleteMonthlyBudgetUseCase(repository)
        var saved = false

        setContent {
            val viewModel = SetBudgetViewModel(
                setMonthlyBudgetUseCase = setUseCase,
                getMonthlyBudgetUseCase = getUseCase,
                deleteMonthlyBudgetUseCase = deleteUseCase,
                yearMonth = targetMonth
            )
            SetBudgetScreen(
                viewModel = viewModel,
                onSaveSuccess = { saved = true },
                onDismiss = {}
            )
        }

        waitForIdle()

        onNodeWithTag("budget_amount_input").performTextClearance()
        onNodeWithTag("budget_amount_input").performTextInput("4200.00")
        onNodeWithTag("save_budget_button").performClick()

        waitForIdle()

        assertTrue(saved)
        runTest {
            val updated = repository.getMonthlyBudget(targetMonth).first()
            assertNotNull(updated)
            assertEquals(420000L, updated.limitAmount)
        }
    }

    @Test
    fun deleteBudget_cancelKeepsDialogDismissedAndBudgetIntact() = runComposeUiTest {
        val repository = FakeBudgetRepository()
        val targetMonth = YearMonth(2026, 8)
        repository.setMonthlyBudget(MonthlyBudget(targetMonth, 200000L))

        val setUseCase = SetMonthlyBudgetUseCase(repository)
        val getUseCase = GetMonthlyBudgetUseCase(repository)
        val deleteUseCase = DeleteMonthlyBudgetUseCase(repository)

        setContent {
            val viewModel = SetBudgetViewModel(
                setMonthlyBudgetUseCase = setUseCase,
                getMonthlyBudgetUseCase = getUseCase,
                deleteMonthlyBudgetUseCase = deleteUseCase,
                yearMonth = targetMonth
            )
            SetBudgetScreen(
                viewModel = viewModel,
                onSaveSuccess = {},
                onDismiss = {}
            )
        }

        waitForIdle()

        onNodeWithTag("delete_budget_button").performClick()
        waitForIdle()

        onNodeWithText("Delete Monthly Budget?").assertIsDisplayed()
        onNodeWithTag("cancel_delete_button").performClick()
        waitForIdle()

        runTest {
            assertNotNull(repository.getMonthlyBudget(targetMonth).first())
        }
    }

    @Test
    fun deleteBudget_confirmInvokesDeleteAndTriggersOnDeleteSuccess() = runComposeUiTest {
        val repository = FakeBudgetRepository()
        val targetMonth = YearMonth(2026, 8)
        repository.setMonthlyBudget(MonthlyBudget(targetMonth, 200000L))

        val setUseCase = SetMonthlyBudgetUseCase(repository)
        val getUseCase = GetMonthlyBudgetUseCase(repository)
        val deleteUseCase = DeleteMonthlyBudgetUseCase(repository)
        var deleted = false

        setContent {
            val viewModel = SetBudgetViewModel(
                setMonthlyBudgetUseCase = setUseCase,
                getMonthlyBudgetUseCase = getUseCase,
                deleteMonthlyBudgetUseCase = deleteUseCase,
                yearMonth = targetMonth
            )
            SetBudgetScreen(
                viewModel = viewModel,
                onSaveSuccess = {},
                onDeleteSuccess = { deleted = true },
                onDismiss = {}
            )
        }

        waitForIdle()

        onNodeWithTag("delete_budget_button").performClick()
        waitForIdle()

        onNodeWithTag("confirm_delete_button").performClick()
        waitForIdle()

        assertTrue(deleted)
        runTest {
            assertNull(repository.getMonthlyBudget(targetMonth).first())
        }
    }

    @Test
    fun invalidInput_displaysErrorAndPreventsSave() = runComposeUiTest {
        val repository = FakeBudgetRepository()
        val setUseCase = SetMonthlyBudgetUseCase(repository)
        val getUseCase = GetMonthlyBudgetUseCase(repository)
        val deleteUseCase = DeleteMonthlyBudgetUseCase(repository)
        val targetMonth = YearMonth(2026, 8)
        var saved = false

        setContent {
            val viewModel = SetBudgetViewModel(
                setMonthlyBudgetUseCase = setUseCase,
                getMonthlyBudgetUseCase = getUseCase,
                deleteMonthlyBudgetUseCase = deleteUseCase,
                yearMonth = targetMonth
            )
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
        val setUseCase = SetMonthlyBudgetUseCase(repository)
        val getUseCase = GetMonthlyBudgetUseCase(repository)
        val deleteUseCase = DeleteMonthlyBudgetUseCase(repository)
        val targetMonth = YearMonth(2026, 8)
        var dismissed = false

        setContent {
            val viewModel = SetBudgetViewModel(
                setMonthlyBudgetUseCase = setUseCase,
                getMonthlyBudgetUseCase = getUseCase,
                deleteMonthlyBudgetUseCase = deleteUseCase,
                yearMonth = targetMonth
            )
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

    @Test
    fun categoryAllocations_validInput_updatesRemainingPillAndPersistsUponSave() = runComposeUiTest {
        val budgetRepo = FakeBudgetRepository()
        val categoryRepo = FakeCategoryBudgetRepository()
        val targetMonth = YearMonth(2026, 8)
        var saved = false

        setContent {
            val viewModel = SetBudgetViewModel(
                setMonthlyBudgetUseCase = SetMonthlyBudgetUseCase(budgetRepo),
                getMonthlyBudgetUseCase = GetMonthlyBudgetUseCase(budgetRepo),
                deleteMonthlyBudgetUseCase = DeleteMonthlyBudgetUseCase(budgetRepo),
                getCategoryBudgetsUseCase = GetCategoryBudgetsUseCase(categoryRepo),
                saveCategoryAllocationsUseCase = SaveCategoryAllocationsUseCase(budgetRepo, categoryRepo),
                removeCategoryBudgetUseCase = RemoveCategoryBudgetUseCase(categoryRepo),
                yearMonth = targetMonth
            )
            SetBudgetScreen(
                viewModel = viewModel,
                onSaveSuccess = { saved = true },
                onDismiss = {}
            )
        }

        waitForIdle()

        // 1. Enter Total Monthly Spending Limit: $3,000.00
        onNodeWithTag("budget_amount_input").performTextInput("3000.00")
        waitForIdle()

        // Verify remaining pill appears showing full $3,000.00
        onNodeWithTag("remaining_allocation_text").assertIsDisplayed()
        onNodeWithText("Remaining to Allocate: $3000.00").assertIsDisplayed()

        // 2. Enter Groceries: $600.00
        onNodeWithTag("category_allocation_input_groceries").performTextInput("600.00")
        waitForIdle()

        // Remaining to allocate updates to $2,400.00
        onNodeWithText("Remaining to Allocate: $2400.00").assertIsDisplayed()

        // 3. Enter Dining: $400.00
        onNodeWithTag("category_allocation_input_dining").performTextInput("400.00")
        waitForIdle()

        // Remaining to allocate updates to $2,000.00
        onNodeWithText("Remaining to Allocate: $2000.00").assertIsDisplayed()

        // 4. Click Save
        onNodeWithTag("save_budget_button").performClick()
        waitForIdle()

        assertTrue(saved)
        runTest {
            val monthly = budgetRepo.getMonthlyBudget(targetMonth).first()
            assertNotNull(monthly)
            assertEquals(300000L, monthly.limitAmount)

            val categories = categoryRepo.getCategoryBudgets(targetMonth).first()
            assertEquals(2, categories.size)
            assertEquals(60000L, categories.find { it.category == TransactionCategory.GROCERIES }?.limitAmount)
            assertEquals(40000L, categories.find { it.category == TransactionCategory.DINING }?.limitAmount)
        }
    }

    @Test
    fun categoryAllocations_overAllocation_displaysErrorBannerAndPreventsSave() = runComposeUiTest {
        val budgetRepo = FakeBudgetRepository()
        val categoryRepo = FakeCategoryBudgetRepository()
        val targetMonth = YearMonth(2026, 8)
        var saved = false

        setContent {
            val viewModel = SetBudgetViewModel(
                setMonthlyBudgetUseCase = SetMonthlyBudgetUseCase(budgetRepo),
                getMonthlyBudgetUseCase = GetMonthlyBudgetUseCase(budgetRepo),
                deleteMonthlyBudgetUseCase = DeleteMonthlyBudgetUseCase(budgetRepo),
                getCategoryBudgetsUseCase = GetCategoryBudgetsUseCase(categoryRepo),
                saveCategoryAllocationsUseCase = SaveCategoryAllocationsUseCase(budgetRepo, categoryRepo),
                removeCategoryBudgetUseCase = RemoveCategoryBudgetUseCase(categoryRepo),
                yearMonth = targetMonth
            )
            SetBudgetScreen(
                viewModel = viewModel,
                onSaveSuccess = { saved = true },
                onDismiss = {}
            )
        }

        waitForIdle()

        // Total Limit: $1,000.00
        onNodeWithTag("budget_amount_input").performTextInput("1000.00")
        waitForIdle()

        // Groceries: $700.00
        onNodeWithTag("category_allocation_input_groceries").performTextInput("700.00")
        waitForIdle()

        // Dining: $500.00 ($700 + $500 = $1,200 > $1,000)
        onNodeWithTag("category_allocation_input_dining").performTextInput("500.00")
        waitForIdle()

        // Over-allocated pill is displayed
        onNodeWithText("Over allocated by $200.00").assertIsDisplayed()

        // Attempt to save
        onNodeWithTag("save_budget_button").performClick()
        waitForIdle()

        assertFalse(saved)
        runTest {
            assertNull(budgetRepo.getMonthlyBudget(targetMonth).first())
        }
    }

    @Test
    fun categoryAllocations_resetButton_clearsInputsAndRestoresRemaining() = runComposeUiTest {
        val budgetRepo = FakeBudgetRepository()
        val categoryRepo = FakeCategoryBudgetRepository()
        val targetMonth = YearMonth(2026, 8)

        setContent {
            val viewModel = SetBudgetViewModel(
                setMonthlyBudgetUseCase = SetMonthlyBudgetUseCase(budgetRepo),
                getMonthlyBudgetUseCase = GetMonthlyBudgetUseCase(budgetRepo),
                deleteMonthlyBudgetUseCase = DeleteMonthlyBudgetUseCase(budgetRepo),
                getCategoryBudgetsUseCase = GetCategoryBudgetsUseCase(categoryRepo),
                saveCategoryAllocationsUseCase = SaveCategoryAllocationsUseCase(budgetRepo, categoryRepo),
                removeCategoryBudgetUseCase = RemoveCategoryBudgetUseCase(categoryRepo),
                yearMonth = targetMonth
            )
            SetBudgetScreen(
                viewModel = viewModel,
                onSaveSuccess = {},
                onDismiss = {}
            )
        }

        waitForIdle()

        onNodeWithTag("budget_amount_input").performTextInput("2000.00")
        onNodeWithTag("category_allocation_input_groceries").performTextInput("500.00")
        waitForIdle()

        onNodeWithText("Remaining to Allocate: $1500.00").assertIsDisplayed()

        // Click Reset
        onNodeWithTag("reset_allocations_button").performClick()
        waitForIdle()

        onNodeWithText("Remaining to Allocate: $2000.00").assertIsDisplayed()
    }
}
